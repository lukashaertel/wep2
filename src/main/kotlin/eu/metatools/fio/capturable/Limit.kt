package eu.metatools.fio.capturable

import eu.metatools.fio.data.Vec

/**
 * Returns a capturable instance that is fixed to end after the given time.
 */
infix fun <T> Capturable<T>.limit(duration: Double) = object :
    Capturable<T> {
    override fun capture(args: T, time: Double, origin: Vec, direction: Vec) =
        this@limit.capture(args, time, origin, direction)

    override val start: Double
        get() = this@limit.start

    override val duration: Double
        get() = minOf(this@limit.duration, duration)
}