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
 * @param executor The executor service.
 */
class NetworkClock(
    val network: Network,
    rate: Long = 1L,
    rateUnit: TimeUnit = TimeUnit.SECONDS,
    executor: ScheduledExecutorService = ScheduledThreadPoolExecutor(1)
) : AutoCloseable, Clock {
    /**
     * The current delta time for the coordinator.
     */
    var currentDeltaTime = network.deltaTime()
        private set

    /**
     * The current synchronized time.
     */
    override val time get() = System.currentTimeMillis() + currentDeltaTime

    /**
     * Executor handle, run periodically.
     */
    private val handle = executor.scheduleAtFixedRate({
        currentDeltaTime = network.deltaTime()
    }, 0L, rate, rateUnit)

    override fun close() {
        handle.cancel(true)
    }

}