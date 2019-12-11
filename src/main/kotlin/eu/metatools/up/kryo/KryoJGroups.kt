package eu.metatools.up.kryo

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.util.Pool
import org.jgroups.Address
import org.jgroups.Message
import org.jgroups.blocks.*
import org.jgroups.conf.ClassConfigurator
import org.jgroups.stack.Protocol
import org.jgroups.util.Buffer
import org.jgroups.util.RspList
import org.jgroups.util.Util
import java.io.NotSerializableException
import java.util.concurrent.CompletableFuture

class KryoConfiguredPool(val configureKryo: (Kryo) -> Unit, threadSafe: Boolean) : Pool<Kryo>(threadSafe, true) {
    override fun create() =
        Kryo().also(configureKryo)
}

class KryoOutputPool(threadSafe: Boolean) : Pool<Output>(threadSafe, true) {
    companion object {
        private const val defaultBufferSize = 512

        private const val defaultMaxBufferSize = -1
    }

    override fun create() =
        Output(defaultBufferSize, defaultMaxBufferSize)
}

class KryoInputPool(threadSafe: Boolean) : Pool<Input>(threadSafe, true) {
    override fun create() =
        Input()
}

/**
 * Handles request with deserialization the [Kryo] object.
 */
class KryoRequestHandler(
    val kryoPool: Pool<Kryo>,
    val inputPool: Pool<Input>,
    val block: Message.(Any?) -> Any?
) : RequestHandler {
    override fun handle(msg: Message): Any? {
        // Get resources.
        val kryo = kryoPool.obtain()
        val input = inputPool.obtain()

        // Update buffer.
        input.setBuffer(msg.rawBuffer, msg.offset, msg.length)

        // Delegate to block on result of reading from message.
        val result = block(msg, kryo.readClassAndObject(input))

        // Release resources.
        inputPool.free(input)
        kryoPool.free(kryo)

        // Return the original result.
        return result
    }
}

/**
 * Facade on the [RequestCorrelator] that uses [Kryo] to serializes the responses. ([org.jgroups.blocks.Marshaller] not
 * appropriate, as it enforces writer types and buffer creation).
 */
class KryoRequestCorrelator(
    val kryoPool: Pool<Kryo>,
    val outputPool: Pool<Output>,
    val inputPool: Pool<Input>,
    transport: Protocol?,
    handler: RequestHandler?,
    local_addr: Address?
) : RequestCorrelator(ClassConfigurator.getProtocolId(RequestCorrelator::class.java), transport, handler, local_addr) {
    override fun handleResponse(
        req: Request<*>,
        sender: Address?,
        buf: ByteArray?,
        offset: Int,
        length: Int,
        is_exception: Boolean
    ) {
        try {
            // Get resources.
            val kryo = kryoPool.obtain()
            val input = inputPool.obtain()

            // Update buffer.
            input.setBuffer(buf, offset, length)

            // Receive response.
            req.receiveResponse(kryo.readClassAndObject(input), sender, is_exception)

            // Release resources.
            inputPool.free(input)
            kryoPool.free(kryo)
        } catch (e: Exception) {
            log.error(Util.getMessage("FailedUnmarshallingBufferIntoReturnValue"), e)
            req.receiveResponse(e, sender, true)
        }
    }


    override fun sendReply(
        req: Message,
        req_id: Long,
        reply: Any,
        is_exception: Boolean
    ) {
        // Track if buffer was created from an exception.
        var isException = is_exception

        // Get a new or existing buffer.
        val kryo = kryoPool.obtain()
        val output = outputPool.obtain()

        try {
            // Save object itself.
            kryo.writeClassAndObject(output, reply)
        } catch (t: Throwable) {
            try {
                // Saving object caused an exception, save this then after resetting output.
                output.reset()
                kryo.writeClassAndObject(output, t)
                isException = true
            } catch (not_serializable: NotSerializableException) {
                // Could also not serialize exception, fail fully.
                if (log.isErrorEnabled)
                    log.error(Util.getMessage("FailedMarshallingRsp") + reply + "): not serializable")

                // Release buffer before returning.
                outputPool.free(output)
                kryoPool.free(kryo)
                return
            } catch (tt: Throwable) {
                // Generally failed with an exception exception, fail fully.
                if (log.isErrorEnabled)
                    log.error(Util.getMessage("FailedMarshallingRsp") + reply + "): " + tt)

                // Release buffer before returning.
                outputPool.free(output)
                kryoPool.free(kryo)
                return
            }
        }

        // Copy the bytes, allows the original buffer to be reused.
        // TODO: Would be nice if jgroups could help me out with this. GENERERALLY FUCKED IN GENERAL
        val buffer = output.toBytes()

        // Make the reply object.
        val rsp = req
            .makeReply()
            .setFlag(req.flags)
            .setBuffer(buffer)
            .clearFlag(Message.Flag.RSVP, Message.Flag.INTERNAL) // JGRP-1940

        // Send it as response.
        sendResponse(rsp, req_id, isException)

        // Release output buffer for reusing.
        outputPool.free(output)
        kryoPool.free(kryo)
    }
}

/**
 * Version of [sendMessage] with an object to serialize.
 */
fun <T> MessageDispatcher.sendMessage(
    kryoPool: Pool<Kryo>,
    outputPool: Pool<Output>,
    dest: Address?,
    any: Any?,
    opts: RequestOptions?
): T? {
    // Get resources.
    val kryo = kryoPool.obtain()
    val output = outputPool.obtain()

    // Save object and convert to bytes.
    kryo.writeClassAndObject(output, any)
    val bytes = output.toBytes()

    // Release resources.
    outputPool.free(output)
    kryoPool.free(kryo)

    // Send the data.
    return sendMessage<T>(dest, bytes, 0, bytes.size, opts)
}

/**
 * Version of [sendMessageWithFuture] with an object to serialize.
 */
fun <T> MessageDispatcher.sendMessageWithFuture(
    kryoPool: Pool<Kryo>,
    outputPool: Pool<Output>,
    dest: Address?,
    any: Any?,
    opts: RequestOptions?
): CompletableFuture<T>? {
    // Get resources.
    val kryo = kryoPool.obtain()
    val output = outputPool.obtain()

    // Save object and convert to bytes.
    kryo.writeClassAndObject(output, any)
    val bytes = output.toBytes()

    // Release resources.
    outputPool.free(output)
    kryoPool.free(kryo)

    // Send the data.
    return sendMessageWithFuture<T>(dest, bytes, 0, bytes.size, opts)
}

/**
 * Version of [castMessage] with an object to serialize.
 */
fun <T> MessageDispatcher.castMessage(
    kryoPool: Pool<Kryo>,
    outputPool: Pool<Output>,
    dests: Collection<Address>?,
    any: Any?,
    opts: RequestOptions?
): RspList<T>? {
    // Get resources.
    val kryo = kryoPool.obtain()
    val output = outputPool.obtain()

    // Save object and convert to bytes.
    kryo.writeClassAndObject(output, any)
    val bytes = output.toBytes()

    // Release resources.
    outputPool.free(output)
    kryoPool.free(kryo)

    return castMessage(dests, bytes, 0, bytes.size, opts)
}

/**
 * Version of [castMessageWithFuture] with an object to serialize.
 */
fun <T> MessageDispatcher.castMessageWithFuture(
    kryoPool: Pool<Kryo>,
    outputPool: Pool<Output>,
    dests: Collection<Address>?,
    any: Any?,
    opts: RequestOptions?
): CompletableFuture<RspList<T>>? {
    // Get resources.
    val kryo = kryoPool.obtain()
    val output = outputPool.obtain()

    // Save object and convert to bytes.
    kryo.writeClassAndObject(output, any)
    val bytes = output.toBytes()

    // Release resources.
    outputPool.free(output)
    kryoPool.free(kryo)

    return castMessageWithFuture(dests, Buffer(bytes, 0, bytes.size), opts)
}