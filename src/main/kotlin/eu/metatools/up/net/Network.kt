package eu.metatools.up.net

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import eu.metatools.up.dt.Instruction
import eu.metatools.up.dt.Lx
import eu.metatools.up.notify.Event
import eu.metatools.up.notify.EventList
import eu.metatools.up.notify.Handler
import eu.metatools.up.notify.HandlerList
import org.jgroups.JChannel
import org.jgroups.ReceiverAdapter
import org.jgroups.blocks.MessageDispatcher
import org.jgroups.blocks.RequestOptions
import java.io.InputStream
import java.io.OutputStream

interface Network {
    /**
     * Called on instruction receive.
     */
    val received: Event<Lx, Instruction>

    /**
     * Called on claims changed.
     */
    val claimsChanged: Handler<Set<Short>>

    /**
     * Claims a slot, returns it or throws an exception.
     */
    fun claimSlot(): Short

    /**
     * Releases the slot, must be one of those claimed on this interface.
     */
    fun releaseSlot(id: Short)

    /**
     * Gets the time adjustment to the coordinator.
     */
    fun deltaTime(): Long

    /**
     * Sends an instruction via the adapter.
     */
    fun instruction(id: Lx, instruction: Instruction)

    /**
     * Returns the bundle.
     */
    fun bundle(): Map<Lx, Any?>

    /**
     * Closes the connection.
     */
    fun close()
}

fun makeNetwork(
    cluster: String,
    bundle: () -> Map<Lx, Any?>,
    stack: String = "fast.xml",
    kryo: Kryo = makeKryo()
): Network {
    val channel = JChannel(stack)

    // Connect to cluster.
    channel.connect(cluster)

    // Request options.

    return object : Network {
        /**
         * True if claimed is initialized.
         */
        private var claimedInitialized =
            channel.address == channel.view.coord

        /**s
         * Set of all claimed IDs.
         */
        private val claimed = mutableSetOf<Short>()

        /**
         * Set of own claims.
         */
        private val ownClaims = mutableSetOf<Short>()

        /**
         * Received instruction.
         */
        override val received = EventList<Lx, Instruction>()

        /**
         * Claims changed.
         */
        override val claimsChanged = HandlerList<Set<Short>>()

        /**
         * Universal dispatcher.
         */
        val dispatcher = MessageDispatcher(channel) {
            // Read type from input.
            val input = Input(it.rawBuffer, it.offset, it.length)
            val type = input.readType()

            // Disambiguate message.
            when (type) {
                MessageType.ClaimList -> handleClaimList(input)
                MessageType.ClaimSlot -> handleClaimSlot(input)
                MessageType.ReleaseSlot -> handleReleaseSlot(input)
                MessageType.DeltaTime -> handleDeltaTime(input)
                MessageType.Instruction -> handleInstruction(input)
                MessageType.Bundle -> handleBundle(input)
            }
        }

        private fun handleClaimList(input: Input): List<Short> {
            return claimed.toList()
        }


        private fun handleClaimSlot(input: Input): Short {
            // Iterate all valid IDs, find one not in claims.
            for (id in Short.MIN_VALUE..Short.MAX_VALUE)
                if (id.toShort() !in claimed) {
                    // Add to list of claims, notify and return it.
                    claimed.add(id.toShort())
                    claimsChanged(claimed.toSet())
                    return id.toShort()
                }


            // Throw exception.
            throw IllegalStateException("No ID available.")
        }

        private fun handleReleaseSlot(input: Input): Nothing? {
            // Get ID from arguments.
            val id = input.readShort()

            // Remove from claims, notify and return void null.
            claimed.remove(id)
            claimsChanged(claimed.toSet())
            return null
        }

        private fun handleDeltaTime(input: Input) =
            System.currentTimeMillis()


        private fun handleInstruction(input: Input): Nothing? {
            // Read ID.
            val id = kryo.readObject(input, Lx::class.java)

            // Read instruction.
            val instruction = kryo.readObject(input, Instruction::class.java)

            // Send to handler.
            received(id, instruction)

            // Return null as void.
            return null
        }

        private fun handleBundle(input: Input): Any? {
            val value = bundle()
            val output = Output(0, 65535)
            output.writeInt(value.size, true)
            for ((k, v) in value) {
                kryo.writeObject(output, k)
                kryo.writeClassAndObject(output, v)
            }
            return output.toBytes()
        }


        override fun claimSlot(): Short {
            // If claims not initialized yet, retrieve from coordinator and then add to set.
            if (!claimedInitialized) {
                val output = Output(1)
                output.writeType(MessageType.ClaimList)
                dispatcher
                    .sendMessage<List<Short>>(channel.view.coord, output.buffer, 0, output.position(), RequestOptions.SYNC())
                    .forEach {
                        claimed.add(it)
                    }

            }

            // Make message.
            val output = Output(1)
            output.writeType(MessageType.ClaimSlot)

            // Send for response.
            val response = dispatcher.castMessage<Short>(
                null, output.buffer, 0, output.position(),
                RequestOptions.SYNC()
            )

            // Assert consistency.
            response.forEach { k, v ->
                require(v.value == response.first) { "Inconsistent claim table on $k" }
            }

            // Add to own claims.
            ownClaims.add(response.first)

            // Return the actual value.
            return response.first
        }

        override fun releaseSlot(id: Short) {
            // If actually own claim, remove.
            if (ownClaims.remove(id)) {
                // Create message.
                val output = Output(3)
                output.writeType(MessageType.ReleaseSlot)
                output.writeShort(id.toInt())

                // Send for no-response, but block.
                val options = RequestOptions.SYNC()
                dispatcher.castMessage<Short>(null, output.buffer, 0, output.position(), options)
            }
        }

        override fun deltaTime(): Long {
            // Create data.
            val output = Output(1)
            output.writeType(MessageType.DeltaTime)

            // Track beginning time.
            val begin = System.currentTimeMillis()

            // Retrieve server time via synchronous send.
            val options = RequestOptions.SYNC()
            val remote = dispatcher.sendMessage<Long>(channel.view.coord, output.buffer, 0, output.position(), options)

            // Track end time.
            val end = System.currentTimeMillis()

            // Adjusted is remote plus time passed since remote generated the message, i.e., RTT over two.
            val adjusted = remote + (end - begin) / 2

            // Delta is difference from local to actual remote time.
            return adjusted - end
        }

        override fun instruction(id: Lx, instruction: Instruction) {
            // Generate output.
            val output = Output(0, 65535)
            output.writeType(MessageType.Instruction)
            kryo.writeObject(output, id)
            kryo.writeObject(output, instruction)

            // Create options.
            val options = RequestOptions.ASYNC().exclusionList(channel.address)

            // Cast to all.
            dispatcher.castMessage<Unit>(null, output.buffer, 0, output.position(), options)
        }

        override fun bundle(): Map<Lx, Any?> {
            // Self-bundle.
            if (channel.view.coord == channel.address)
                return bundle()

            // Create request message.
            val output = Output(1)
            output.writeType(MessageType.Bundle)

            // Execute request for response.
            val options = RequestOptions.SYNC()
            val input = dispatcher
                .sendMessage<ByteArray>(channel.view.coord, output.buffer, 0, output.position(), options)
                .let(::Input)

            // Get count of entries, create mutable map.
            val count = input.readInt(true)
            val result = HashMap<Lx, Any?>(count, 1f)

            // Read all entries.
            repeat(count) {
                val key = kryo.readObject(input, Lx::class.java)
                val value = kryo.readClassAndObject(input)
                result[key] = value
            }

            // Return the result.
            return result
        }

        override fun close() {
            // Release all claims.
            while (ownClaims.isNotEmpty())
                releaseSlot(ownClaims.first())

            // Disconnect and close.
            channel.disconnect()
            channel.close()
        }
    }
}

