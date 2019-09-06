package eu.metatools.f2d.context

/**
 * An object that has a [duration].
 */
interface Timed {
    /**
     * The time when this object starts, defaults to starting at the time origin (0.0).
     */
    val start: Double get() = 0.0

    /**
     * How long the object lives in total after it starts, defaults to positive infinity.
     */
    val duration: Double get() = Double.POSITIVE_INFINITY
}

/**
 * gets the end time as sum of [Timed.start] and [Timed.duration].
 */
val Timed.end
    get() =
        start + duration