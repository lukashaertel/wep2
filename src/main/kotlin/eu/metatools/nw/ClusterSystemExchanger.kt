package eu.metatools.nw

import eu.metatools.wep2.system.StandardInitializer
import eu.metatools.wep2.system.StandardName
import eu.metatools.wep2.system.StandardSystem
import eu.metatools.wep2.tools.Time
import org.jgroups.JChannel
import org.jgroups.Message
import org.jgroups.Receiver
import java.io.InputStream
import java.io.OutputStream

interface Encoding<N, P> {
    /**
     * Writes the initializer.
     */
    fun writeInitializer(standardInitializer: StandardInitializer<N, P>): ByteArray

    /**
     * Reads the initializer.
     */
    fun readInitializer(byteArray: ByteArray): StandardInitializer<N, P>

    /**
     * Writes the instruction.
     */
    fun writeInstruction(instruction: Triple<StandardName<N>, Time, Any?>): ByteArray

    /**
     * Reads the instruction.
     */
    fun readInstruction(byteArray: ByteArray): Triple<StandardName<N>, Time, Any?>
}


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

            // Read instruction.
            val (name, time, args) = encoding.readInstruction(msg.buffer)

            // Receive it.
            target.receive(name, time, args)
        }

        override fun setState(input: InputStream) {
            // Read initializer.
            val initializer = input.use {
                encoding.readInitializer(it.readAllBytes())
            }

            // Create target from state exchange.
            target = StandardSystem(parameter, initializer)

        }

        override fun getState(output: OutputStream) {
            // Write initializer from save.
            output.use {
                it.write(encoding.writeInitializer(target.save()))
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
        // Create output, send it to everyone.
        channel.send(null, encoding.writeInstruction(Triple(name, time, args)))
    }

    // Return target and closing method.
    return target to AutoCloseable {
        // Reset receiver, disconnect and close.
        channel.receiver = null
        channel.disconnect()
        channel.close()
    }
}