package eu.metatools.up.dt

/**
 * An abstract global clock.
 */
interface Clock {
    /**
     * The current time.
     */
    val time: Long
}