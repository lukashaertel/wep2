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
    rate: Long = 1L,
    rateUnit: TimeUnit = TimeUnit.SECONDS
) : AutoCloseable, Clock {
    /**
     * The current delta time for the coordinator.
     */
    var currentDeltaTime = network.offset()
        private set

    /**
     * The current synchronized time.
     */
    override val time get() = System.currentTimeMillis() + currentDeltaTime

    /**
     * Executor handle, run periodically.
     */
    private val handle = network.executor.scheduleAtFixedRate({
        currentDeltaTime = network.offset()
    }, 0L, rate, rateUnit)

    override fun close() {
        handle.cancel(true)
    }
}