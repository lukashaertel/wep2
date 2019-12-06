package eu.metatools.up.net

import com.esotericsoftware.kryo.Kryo
import eu.metatools.up.dt.Instruction
import eu.metatools.up.dt.Lx
import eu.metatools.up.kryo.*
import eu.metatools.up.lang.never
import org.jgroups.JChannel
import org.jgroups.blocks.MessageDispatcher
import org.jgroups.blocks.RequestOptions
import org.jgroups.stack.Protocol
import java.lang.UnsupportedOperationException
import java.util.*
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.collections.HashSet
import kotlin.concurrent.thread

interface Network {
    /**
     * Executor for operations concerned with network.
     */
    val executor: ScheduledExecutorService
        get() = throw UnsupportedOperationException("Network does not support this operation.")

    /**
     * True if coordinating.
     */
    val isCoordinating: Boolean

    /**
     * True if this [id] is still claimed.
     */
    fun isClaimed(id: Short): Boolean =
        throw UnsupportedOperationException("Network does not support this operation.")


    /**
     * Marks the UUID as claimed by this node. Invocation marks the UUID as still claiming.
     */
    fun touch(uuid: UUID): Short

    /**
     * Gets the time adjustment to the coordinator.
     */
    fun offset(): Long

    /**
     * Sends an instruction via the adapter.
     */
    fun instruction(instruction: Instruction)

    /**
     * Returns the bundle.
     */
    fun bundle(): Map<Lx, Any?>

    /**
     * Closes the connection.
     */
    fun close()
}

/**
 * Makes the network.
 *
 * @param cluster The cluster name.
 * @param onBundle The bundling method.
 * @param onReceive The receive method.
 * @param stack The stack file to use for JGroups.
 * @param kryo The Kryo configuration to use.
 */
fun makeNetwork(
    cluster: String,
    onBundle: () -> Map<Lx, Any?>,
    onReceive: (Instruction) -> Unit,
    stack: String = "fast.xml",
    leaseTime: Long = 30L,
    leaseTimeUnit: TimeUnit = TimeUnit.MINUTES,
    requestTimeout: Long = 1000L,
    kryo: Kryo = makeUpKryo()
): Network {
    val channel = JChannel(stack)

    // Connect to cluster.
    channel.connect(cluster)

    // Protect faults from unmanaged termination.
    Runtime.getRuntime().addShutdownHook(thread(false) {
        if (!channel.isClosed)
            channel.close()
    })

    return object : Network {
        private val sync get() = RequestOptions.SYNC().timeout(requestTimeout)

        private val otherAsync get() = RequestOptions.ASYNC().exclusionList(channel.address)

        private val coord get() = channel.view.coord

        /**
         * True if claimed is initialized.
         */
        private var claimedInitialized = isCoordinating

        /**s
         * Set of all claimed IDs.
         */
        private val claimed = hashMapOf<UUID, Claim>()

        /**
         * Universal dispatcher.
         */
        val dispatcher = MessageDispatcher().apply {
            // Set request handler.
            setRequestHandler<MessageDispatcher>(KryoRequestHandler(kryo) {
                // Disambiguate message.
                when (it) {
                    is NetReqClaims -> onReqClaims()
                    is NetReqOffset -> onReqOffset()
                    is NetReqBundle -> onReqBundle()
                    is NetInstruction -> onInstruction(it)
                    is NetTouch -> onTouch(it)
                    else -> error("Unknown top level message $it")
                }
            })

            // Set target channel.
            setChannel<MessageDispatcher>(channel)

            // Set replacement request correlator, starts the dispatcher.
            setCorrelator<MessageDispatcher>(
                KryoRequestCorrelator(kryo, protocolAdapter as Protocol, this, channel.address)
            )
        }

        private fun onReqClaims(): Map<UUID, Claim> {
            return claimed.toMap()
        }

        private fun onReqOffset(): Long {
            return System.currentTimeMillis()
        }

        private fun onReqBundle(): Map<Lx, Any?> {
            return onBundle()
        }

        private fun onInstruction(req: NetInstruction) {
            onReceive(req.instruction)
        }

        private fun onTouch(req: NetTouch): Claim {
            // Remove inactive leases.
            claimed.entries.removeIf { (k, v) ->
                if (v.expires < req.time)
                    println("$k expired")
                v.expires < req.time
            }

            // Get expiry time.
            val expires = req.time + leaseTimeUnit.toMillis(leaseTime)

            // Update or insert.
            return claimed.compute(req.uuid) { _, existing ->
                if (existing == null) {
                    // Get all occupied slots.
                    val occupied = claimed.values.mapTo(HashSet()) { it.id }

                    // Find new non-occupied slot.
                    val new = (Short.MIN_VALUE..Short.MAX_VALUE)
                        .asSequence()
                        .map(Int::toShort)
                        .first {
                            it !in occupied
                        }

                    // Return it with expiry.
                    Claim(new, expires)
                } else {
                    // Return existing with expiry.
                    existing.renew(expires)
                }
            } ?: never
        }

        /**
         * The value of the executor or null if [executor] was not called yet.
         */
        private var executorValue: ScheduledThreadPoolExecutor? = null

        override val executor by lazy {
            // Create new executor, also assign to non-lazy initialized field.
            ScheduledThreadPoolExecutor(4).also {
                executorValue = it
            }
        }

        override val isCoordinating
            get() = channel.address == channel.view.coord

        private fun assertClaimedPresent() {
            // Claims present, status ok.
            if (claimedInitialized)
                return

            // Get claim table safe.
            val claimTable = dispatcher.sendMessage<Map<UUID, Claim>>(kryo, coord, NetReqClaims, sync) ?: never

            // Update claim table.
            claimed.putAll(claimTable)
            claimedInitialized = true
        }

        override fun isClaimed(id: Short): Boolean {
            // Assert that claimed table is present.
            assertClaimedPresent()

            // Return true if any of the values claims the ID.
            return claimed.values.any { it.id == id }
        }

        override fun touch(uuid: UUID): Short {
            // Assert that claimed table is present.
            assertClaimedPresent()

            // Get current time.
            val now = System.currentTimeMillis()

            // Send claim to all participants, find one that's ok.
            val allResults = dispatcher.castMessage<Claim>(kryo, null, NetTouch(uuid, now), sync) ?: never
            val results = allResults.values.filter { it.wasReceived() && !it.wasUnreachable() && !it.hasException() }
            val result = requireNotNull(results.firstOrNull()) {
                "No result received without exception."
            }

            // Assert consistency.
            results.forEach { item ->
                require(item.value == result.value) {
                    "Inconsistent claim table, ${item.value} received with ${results.map { it.value }}."
                }
            }

            // Add claim.
            claimed[uuid] = result.value

            // Return the ID part of the claim.
            return result.value.id
        }

        override fun offset(): Long {
            // No need to synchronize with self.
            if (isCoordinating)
                return 0L

            // Retrieve server time via synchronous send and get RTT.
            val begin = System.currentTimeMillis()
            val result = dispatcher.sendMessage<Long>(kryo, coord, NetReqOffset, sync) ?: never
            val end = System.currentTimeMillis()

            // Adjusted is remote plus time passed since remote generated the message, i.e., RTT over two.
            val adjusted = result + (end - begin) / 2L

            // Delta is difference from local to actual remote time.
            return adjusted - end
        }

        override fun instruction(instruction: Instruction) {
            // Cast to all nodes except self.
            dispatcher.castMessage<Unit>(kryo, null, NetInstruction(instruction), otherAsync)
        }

        override fun bundle(): Map<Lx, Any?> {
            // Self-bundle if coordinating.
            if (channel.view.coord == channel.address)
                return onBundle()

            // Send request for bundle to coordinator.
            return dispatcher.sendMessage(kryo, coord, NetReqBundle, sync) ?: never
        }

        override fun close() {
            // Shutdown the executor if it was created.
            executorValue?.shutdown()

            // Disconnect and close.
            channel.disconnect()
            channel.close()
        }
    }
}