package eu.metatools.nw

import eu.metatools.nw.encoding.Encoding
import eu.metatools.wep2.aspects.saveToMap
import eu.metatools.wep2.entity.Entity
import eu.metatools.wep2.entity.SI
import eu.metatools.wep2.storage.restoreBy
import eu.metatools.wep2.system.Concurrency
import eu.metatools.wep2.system.StandardSystem
import eu.metatools.wep2.tools.Time
import eu.metatools.wep2.util.ComparablePair
import eu.metatools.wep2.util.collections.ObservableMapListener
import eu.metatools.wep2.util.listeners.Listener
import eu.metatools.wep2.util.listeners.MapListener
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
    val system: StandardSystem<N>

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
    propsName: String = "fast.xml",
    stateTimeout: Long = 10_000L,
    playerSelfListener: Listener<Unit, ComparablePair<Short, Short>?> = Listener.EMPTY,
    playerCountListener: Listener<Unit, Short> = Listener.EMPTY,
    indexListener: ObservableMapListener<SI, Entity<N, Time, SI>> = MapListener.EMPTY
): Net<N, P> {
    // Result variable, will receive calls during connection.
    lateinit var target: StandardSystem<N>

    // Create channel with the given properties.
    val channel = JChannel(propsName)
    channel.discardOwnMessages = true

    // Store for applied time delta. TODO: With state echange solution, ping is not included.
    var delta = -System.currentTimeMillis()

    // Configure receiver.
    channel.receiver(object : Receiver {
        override fun receive(msg: Message) {
            // Create input stream from buffer.
            ByteArrayInputStream(msg.rawBuffer, msg.offset, msg.length).use {
                // Read instruction from stream and receive it.
                val (name, time, args) = encoding.readInstruction(it)
                target.receive(name, time, args)
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
                }


                // Read initializer.
                val data = encoding.readInitializer(it)
                for ((k, v) in data) println("\t$k=$v")
                // Create target from state exchange.
                target = restoreBy(data::get) { restore ->
                    StandardSystem(
                        restore, { time -> time + delta }, Concurrency.SYNC,
                        playerSelfListener,
                        playerCountListener,
                        indexListener
                    )
                }
                println("Restored $target")
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

                // Save data of the target.
                println("Storing $target")
                val data = target.saveToMap()
                for ((k, v) in data) println("\t$k=$v")


                // Write data to state exchange.
                encoding.writeInitializer(it, data)
            }
        }
    })


    // Connect, if connecting to a non-empty cluster, load from state.
    channel.connect(cluster)
    if (channel.view.size() > 1)
        channel.getState(null, stateTimeout)
    else
        target = StandardSystem(
            null, { time -> time + delta }, Concurrency.SYNC,
            playerSelfListener,
            playerCountListener,
            indexListener
        )

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
        override val system: StandardSystem<N>
            // Return initialized target.
            get() = target

        override fun stop() {
            // Reset receiver, disconnect and close.
            channel.receiver = null
            channel.disconnect()
            channel.close()
        }
    }
}