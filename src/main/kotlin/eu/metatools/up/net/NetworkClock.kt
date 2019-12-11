package eu.metatools.up.net

import eu.metatools.up.dt.Clock
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Network delta time coordinator.
 * @property network The network to coordinate on.
 * @param rate The rate at which to update, defaults to a second.
 * @param rateUnit The unit of the rate, defaults to [TimeUnit.SECONDS].
 */
class NetworkClock(
    val network: Network,
    changed: ((Long, Long) -> Unit)? = null,
    rate: Long = 1L,
    rateUnit: TimeUnit = TimeUnit.SECONDS
) : AutoCloseable, Clock {
    /**
     * The current delta time for the coordinator.
     */
    var currentDeltaTime = network.ping()
        private set

    /**
     * The current synchronized time.
     */
    override val time get() = System.currentTimeMillis() + currentDeltaTime

    /**
     * Executor handle, run periodically.
     */
    private val handle = network.executor.scheduleAtFixedRate({
        val old = currentDeltaTime
        val new = network.ping()
        currentDeltaTime = new
        if (old != new)
            changed?.invoke(old, new)
    }, 0L, rate, rateUnit)

    override fun close() {
        handle.cancel(true)
    }
}