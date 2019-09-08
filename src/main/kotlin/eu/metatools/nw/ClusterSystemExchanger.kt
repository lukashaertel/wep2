package eu.metatools.nw

import eu.metatools.nw.encoding.Encoding
import eu.metatools.wep2.system.StandardSystem
import org.jgroups.JChannel
import org.jgroups.Message
import org.jgroups.Receiver
import java.io.*

fun <N, P> enter(
    encoding: Encoding<N, P>,
    cluster: String,
    parameter: P,
    propsName: String = "fast.xml",
    stateTimeout: Long = 10_000L
): Pair<StandardSystem<N, P>, AutoCloseable> {
    // Result variable, will receive calls during connection.
    lateinit var target: StandardSystem<N, P>

    // Create channel with the given properties.
    val channel = JChannel(propsName)

    // Configure receiver.
    channel.receiver(object : Receiver {
        override fun receive(msg: Message) {
            // Skip local messages.
            if (msg.src == channel.address)
                return

            // Create input stream from buffer.
            ByteArrayInputStream(msg.rawBuffer, msg.offset, msg.length).use {
                // Read instruction from stream.
                encoding.readInstruction(it).let { (name, time, args) ->
                    // Feed into target.
                    target.receive(name, time, args)
                }
            }
        }

        override fun setState(input: InputStream) {
            // Use input stream.
            input.use {
                // Read initializer.
                val initializer = encoding.readInitializer(it)

                // Create target from state exchange.
                target = StandardSystem(parameter, initializer)
            }
        }

        override fun getState(output: OutputStream) {
            // Write initializer from save.
            output.use {
                // Save initializer.
                val initializer = target.save()

                // Write initializer to state exchange.
                encoding.writeInitializer(it, initializer)
            }
        }
    })

    // Connect, if connecting to a non-empty cluster, load from state.
    channel.connect(cluster)
    if (channel.view.size() > 1)
        channel.getState(null, stateTimeout)
    else
        target = StandardSystem(parameter, null)

    // Connect outgoing messages of the cluster.
    target.register { name, time, args ->
        // Create output stream to write to.
        val data = ByteArrayOutputStream().use {
            // Write instruction to buffer.
            encoding.writeInstruction(it, Triple(name, time, args))

            // Return the byte array from the given
            it.toByteArray()
        }

        // Send data to all addresses..
        channel.send(null, data)
    }

    // Return target and closing method.
    return target to AutoCloseable {
        // Reset receiver, disconnect and close.
        channel.receiver = null
        channel.disconnect()
        channel.close()
    }
}