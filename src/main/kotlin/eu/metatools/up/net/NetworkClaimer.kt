package eu.metatools.up.net

import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Network claim coordinator.
 * @property network The network to coordinate on.
 * @param rate The rate at which to update, defaults to a second.
 * @param initialDelay The initial delay before retrieval.
 */
class NetworkClaimer(
    val network: Network,
    val uuid: UUID,
    changed: ((Short, Short) -> Unit)? = null,
    rate: Long = 10_000L,
    initialDelay: Long = 0L
) : AutoCloseable {
    /**
     * The current claim for the coordinator, some applications might require this to be constant and should check
     * for updated values.
     */
    var currentClaim: Short = network.touch(uuid)
        private set

    /**
     * Executor handle, run periodically.
     */
    private val handle = network.executor.scheduleAtFixedRate({
        val old = currentClaim
        val new = network.touch(uuid)
        currentClaim = new
        if (old != new)
            changed?.invoke(old, new)
    }, initialDelay, rate, TimeUnit.MILLISECONDS)

    override fun close() {
        handle.cancel(true)
    }
}