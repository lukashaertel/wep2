package eu.metatools.up.net

import com.esotericsoftware.kryo.Kryo
import eu.metatools.up.dt.Instruction
import eu.metatools.up.dt.Lx
import eu.metatools.up.kryo.*
import eu.metatools.up.lang.never
import org.jgroups.Address
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
     * Gets the time adjustment to the coordinator. Updates the sign-off.
     */
    fun ping(): Long

    /**
     * Gets the sign-off time.
     */
    fun signOff(): Long?

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
    signOffSlack: Long = 5_000L,
    configureKryo: (Kryo) -> Unit = {
        setDefaults(it)
        registerKotlinSerializers(it)
        registerUpSerializers(it)
    }
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
        /**
         * Provides instances of [Kryo] with configuration applied.
         */
        private val kryoPool = KryoConfiguredPool(configureKryo, true)

        /**
         * Provides write targets.
         */
        private val outputPool = KryoOutputPool(true)

        /**
         * Provides read sources.
         */
        private val inputPool = KryoInputPool(true)

        /**
         * Request options for synchronous send.
         */
        private val sync get() = RequestOptions.SYNC().timeout(requestTimeout)

        /**
         * Request options for asynchronous send to all other nodes.
         */
        private val otherAsync get() = RequestOptions.ASYNC().exclusionList(channel.address)

        /**
         * The address of the coordinator.
         */
        private val coord get() = requireNotNull(channel.view?.coord) { "No coordinator in channel." }

        /**
         * True if claimed is initialized.
         */
        private var claimedInitialized = isCoordinating

        /**s
         * Set of all claimed IDs.
         */
        private val claimed = hashMapOf<UUID, Claim>()

        /**
         * Set of all times received from offset.
         */
        private val times = hashMapOf<Address, Long>()

        /**
         * Universal dispatcher.
         */
        val dispatcher = MessageDispatcher().apply {
            // Set request handler.
            setRequestHandler<MessageDispatcher>(KryoRequestHandler(kryoPool, inputPool) {
                // Disambiguate message.
                when (it) {
                    is NetReqClaims -> onReqClaims()
                    is NetReqSignOff -> onReqSignOff()
                    is NetReqBundle -> onReqBundle()
                    is NetPing -> onPing(src, it)
                    is NetInstruction -> onInstruction(it)
                    is NetTouch -> onTouch(it)
                    else -> error("Unknown top level message $it")
                }
            })

            // Set target channel.
            setChannel<MessageDispatcher>(channel)

            // Set replacement request correlator, starts the dispatcher.
            setCorrelator<MessageDispatcher>(
                KryoRequestCorrelator(
                    kryoPool, outputPool, inputPool,
                    protocolAdapter as Protocol,
                    this,
                    channel.address
                )
            )
        }

        private fun onReqClaims(): Map<UUID, Claim> {
            return claimed.toMap()
        }

        private fun onReqSignOff(): Long? {
            // Get own time.
            val self = System.currentTimeMillis()

            // Get current set of addresses.
            val current = channel.view.members.toSet()

            // Retain only those addresses.
            times.keys.retainAll(current)

            // Find minimum, if encountering a non-present value, no sign-off yet.
            var result = Long.MAX_VALUE
            for (a in current) {
                // Get time from table or self.
                val time = if (a == channel.address)
                    self
                else
                    times[a]

                // Take minimum, unless not yet set, then return null.
                result = minOf(result, time ?: return null)
            }

            // Return the minimum value, allow for some slack.
            return result - signOffSlack
        }


        private fun onReqBundle(): Map<Lx, Any?> {
            return onBundle()
        }

        private fun onPing(address: Address, req: NetPing): Long {
            times[address] = req.time
            return System.currentTimeMillis()
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
            get() = channel.address == channel.view?.coord

        private fun assertClaimedPresent() {
            // Claims present, status ok.
            if (claimedInitialized)
                return

            // Get claim table safe.
            val claimTable = dispatcher.sendMessage<Map<UUID, Claim>>(
                kryoPool, outputPool, coord, NetReqClaims, sync
            ) ?: never

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
            val allResults = dispatcher.castMessage<Claim>(
                kryoPool, outputPool, null, NetTouch(uuid, now), sync
            ) ?: never
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

        override fun ping(): Long {
            // Get start time.
            val begin = System.currentTimeMillis()

            // Request ping or handle locally.
            val ping = if (isCoordinating)
                onPing(channel.address, NetPing(begin))
            else
                dispatcher.sendMessage<Long>(
                    kryoPool, outputPool, coord, NetPing(begin), sync
                ) ?: never

            // Get end time.
            val end = System.currentTimeMillis()

            // Adjusted is remote plus time passed since remote generated the message, i.e., RTT over two.
            val adjusted = ping + (end - begin) / 2L

            // Delta is difference from local to actual remote time.
            return adjusted - end
        }

        override fun signOff(): Long? {
            // Request sign-off or handle locally.
            return if (isCoordinating)
                onReqSignOff()
            else
                dispatcher.sendMessage(kryoPool, outputPool, coord, NetReqSignOff, sync)
        }

        override fun instruction(instruction: Instruction) {
            // Cast to all nodes except self.
            dispatcher.castMessage<Unit>(
                kryoPool, outputPool, null, NetInstruction(instruction), otherAsync
            )
        }

        override fun bundle(): Map<Lx, Any?> {
            // Request bundle or handle locally.
            return if (isCoordinating)
                onBundle()
            else
                dispatcher.sendMessage(
                    kryoPool, outputPool, coord, NetReqBundle, sync
                ) ?: never
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