package eu.metatools.nw

import eu.metatools.nw.encoding.Encoding
import eu.metatools.wep2.system.StandardName
import eu.metatools.wep2.system.StandardSystem
import eu.metatools.wep2.tools.Time
import org.jgroups.JChannel
import org.jgroups.Message
import org.jgroups.Receiver
import java.io.*

/**
 * Interface for basic network participation.
 */
interface Net<N, P> {
    /**
     * The system that is created or initialized from participation.
     */
    val system: StandardSystem<N, P>

    /**
     * Pushes all buffered instructions to the [system].
     */
    fun update()

    /**
     * Stops participation.
     */
    fun stop()
}

/**
 * Enters a [cluster] with the given binary [encoding]. If creating new, [parameter] is used.
 *
 * @param encoding The binary encoding for encoding messages and state.
 * @param cluster The name of the cluster to join.
 * @param parameter The parameter when creating a new system.
 * @param propsName Specifies what protocol stack is used
 * @param stateTimeout The timeout to use when getting the state.
 */
fun <N, P> enter(
    encoding: Encoding<N, P>,
    cluster: String,
    parameter: P,
    propsName: String = "fast.xml",
    stateTimeout: Long = 10_000L
): Net<N, P> {
    // Result variable, will receive calls during connection.
    lateinit var target: StandardSystem<N, P>

    // Create channel with the given properties.
    val channel = JChannel(propsName)
    channel.discardOwnMessages = true

    // Store for applied time delta. TODO: With state echange solution, ping is not included.
    var delta = -System.currentTimeMillis()

    // Buffer of instructions.
    val buffer = mutableListOf<Triple<StandardName<N>, Time, Any?>>()

    // Configure receiver.
    channel.receiver(object : Receiver {
        override fun receive(msg: Message) {

            // Create input stream from buffer.
            ByteArrayInputStream(msg.rawBuffer, msg.offset, msg.length).use {
                // Read instruction from stream, add to buffer.
                encoding.readInstruction(it).let {
                    synchronized(buffer) {
                        buffer.add(it)
                    }
                }
            }
        }

        override fun setState(input: InputStream) {
            // Get current time for delta adjustment.
            val currentTime = System.currentTimeMillis()

            // Use input stream.
            input.use {
                DataInputStream(it).let { data ->
                    // Read response.
                    val remoteTime = data.readLong()
                    val remoteDelta = data.readLong()

                    // Compute time actually adjusting to.
                    val adjustTo = remoteTime + remoteDelta

                    // Set delta from difference.
                    delta = adjustTo - currentTime
                    println("Delta adjusted to $delta")
                    println("\tCurrent time: $currentTime")
                    println("\tRemote time: $remoteTime")
                    println("\tRemote delta: $remoteDelta")
                }


                // Read initializer.
                val initializer = encoding.readInitializer(it)

                // Create target from state exchange.
                target = StandardSystem.create(parameter, initializer) { it + delta }
            }
        }

        override fun getState(output: OutputStream) {
            // Get current time for delta adjustment.
            val currentTime = System.currentTimeMillis()

            // Write initializer from save.
            output.use {
                // Write data.
                DataOutputStream(it).let { response ->
                    response.writeLong(currentTime)
                    response.writeLong(delta)
                }

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
        target = StandardSystem.create(parameter, null) { it + delta }

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
    return object : Net<N, P> {
        override val system: StandardSystem<N, P>
            // Return initialized target.
            get() = target

        // TODO: Better sync, elements are still flickering while saving.
        override fun update() =
            // Synchronize buffer, receive the copy as a sequence.
            target.receiveAll(synchronized(buffer) {
                buffer.toList().asSequence().also {
                    buffer.clear()
                }
            })

        override fun stop() {
            // Reset receiver, disconnect and close.
            channel.receiver = null
            channel.disconnect()
            channel.close()
        }
    }
}