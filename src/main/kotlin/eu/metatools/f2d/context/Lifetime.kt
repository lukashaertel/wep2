package eu.metatools.f2d.context

interface Lifetime<in T> {
    /**
     * True if this object has started.
     */
    fun hasStarted(args: T, time: Double): Boolean =
        0.0 <= time

    /**
     * True if this object has ended for the time.
     */
    fun hasEnded(args: T, time: Double): Boolean =
        false
}