package eu.metatools.up.kryo

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import org.jgroups.Address
import org.jgroups.Message
import org.jgroups.blocks.*
import org.jgroups.conf.ClassConfigurator
import org.jgroups.stack.Protocol
import org.jgroups.util.Buffer
import org.jgroups.util.Rsp
import org.jgroups.util.RspList
import org.jgroups.util.Util
import java.io.NotSerializableException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.TimeoutException


object KryoOutputPool {
    private const val defaultBufferSize = 512

    private const val defaultMaxBufferSize = -1

    private val reused = ConcurrentLinkedDeque<Output>()

    /**
     * Acquires an output object with reset position.
     */
    fun acquire() =
        // Get existing output and reset it, or create a new one.
        reused.poll()?.also { it.reset() }
            ?: Output(defaultBufferSize, defaultMaxBufferSize)

    /**
     * Releases an output object for reuse.
     */
    fun release(output: Output) =
        // Release it again.
        reused.offer(output)

    /**
     * Acquires a buffer while running [block].
     */
    inline fun <R> with(block: (Output) -> R): R {
        val output = acquire()
        try {
            return block(output)
        } finally {
            release(output)
        }
    }
}

/**
 * Handles request with deserialization the [Kryo] object.
 */
class KryoRequestHandler(val kryo: Kryo, val block: (Any?) -> Any?) : RequestHandler {
    override fun handle(msg: Message) =
        // Delegate to block on result of reading from message.
        block(kryo.readClassAndObject(Input(msg.rawBuffer, msg.offset, msg.length)))
}

/**
 * Facade on the [RequestCorrelator] that uses [Kryo] to serializes the responses. ([org.jgroups.blocks.Marshaller] not
 * appropriate, as it enforces writer types and buffer creation).
 */
class KryoRequestCorrelator(val kryo: Kryo, transport: Protocol?, handler: RequestHandler?, local_addr: Address?) :
    RequestCorrelator(ClassConfigurator.getProtocolId(RequestCorrelator::class.java), transport, handler, local_addr) {

    override fun handleResponse(
        req: Request<*>,
        sender: Address?,
        buf: ByteArray?,
        offset: Int,
        length: Int,
        is_exception: Boolean
    ) {
        try {
            req.receiveResponse(kryo.readClassAndObject(Input(buf, offset, length)), sender, is_exception)
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
        val output = KryoOutputPool.acquire()

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
                KryoOutputPool.release(output)
                return
            } catch (tt: Throwable) {
                // Generally failed with an exception exception, fail fully.
                if (log.isErrorEnabled)
                    log.error(Util.getMessage("FailedMarshallingRsp") + reply + "): " + tt)

                // Release buffer before returning.
                KryoOutputPool.release(output)
                return
            }
        }

        // Make the reply object.
        val rsp = req
            .makeReply()
            .setFlag(req.flags)
            .setBuffer(output.buffer, 0, output.position())
            .clearFlag(Message.Flag.RSVP, Message.Flag.INTERNAL) // JGRP-1940

        // Send it as response.
        sendResponse(rsp, req_id, isException)

        // Release output buffer for reusing.
        KryoOutputPool.release(output)
    }
}

/**
 * Version of [sendMessage] with an object to serialize.
 */
fun <T> MessageDispatcher.sendMessage(kryo: Kryo, dest: Address?, any: Any?, opts: RequestOptions?): T? {
    return KryoOutputPool.with {
        // Write message.
        kryo.writeClassAndObject(it, any)

        // Return result of send message.
        sendMessage<T>(dest, it.buffer, 0, it.position(), opts)
    }
}

/**
 * Version of [sendMessageWithFuture] with an object to serialize.
 */
fun <T> MessageDispatcher.sendMessageWithFuture(
    kryo: Kryo,
    dest: Address?,
    any: Any?,
    opts: RequestOptions?
): CompletableFuture<T>? {
    return KryoOutputPool.with {
        // Write message.
        kryo.writeClassAndObject(it, any)

        // Return result of sending with future.
        sendMessageWithFuture<T>(dest, it.buffer, 0, it.position(), opts)
    }
}

/**
 * Version of [castMessage] with an object to serialize.
 */
fun <T> MessageDispatcher.castMessage(
    kryo: Kryo,
    dests: Collection<Address>?,
    any: Any?,
    opts: RequestOptions?
): RspList<T>? {
    KryoOutputPool.with {
        // Write message.
        kryo.writeClassAndObject(it, any)

        // Return result of casting.
        return castMessage(dests, it.buffer, 0, it.position(), opts)
    }
}

/**
 * Version of [castMessageWithFuture] with an object to serialize.
 */
fun <T> MessageDispatcher.castMessageWithFuture(
    kryo: Kryo,
    dests: Collection<Address>?,
    any: Any?,
    opts: RequestOptions?
): CompletableFuture<RspList<T>>? {
    KryoOutputPool.with {
        // Write message.
        kryo.writeClassAndObject(it, any)

        // Return result of casting with future.
        return castMessageWithFuture(dests, Buffer(it.buffer, 0, it.position()), opts)
    }
}