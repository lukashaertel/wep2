package eu.metatools.up.net

import eu.metatools.up.dt.Clock
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Network sign-off coordinator.
 * @property network The network to coordinate on.
 * @param rate The rate at which to update, defaults to a second.
 * @param rateUnit The unit of the rate, defaults to [TimeUnit.SECONDS].
 */
class NetworkSignOff(
    val network: Network,
    changed: ((Long?, Long?) -> Unit)? = null,
    rate: Long = 1L,
    rateUnit: TimeUnit = TimeUnit.SECONDS
) : AutoCloseable {
    /**
     * The current sign-off for the coordinator.
     */
    var currentSignOff = network.signOff()
        private set

    /**
     * Executor handle, run periodically.
     */
    private val handle = network.executor.scheduleAtFixedRate({
        val old = currentSignOff
        val new = network.signOff()
        currentSignOff = new
        if (old != new)
            changed?.invoke(old, new)
    }, 0L, rate, rateUnit)

    override fun close() {
        handle.cancel(true)
    }
}