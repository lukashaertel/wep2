package eu.metatools.f2d.capturable

import eu.metatools.f2d.data.Vec

/**
 * Returns a capturable instance that is offset by the given time.
 */
infix fun <T> Capturable<T>.offset(offset: Double) = object :
    Capturable<T> {
    override fun capture(args: T, time: Double, origin: Vec, direction: Vec) =
        this@offset.capture(args, time - offset, origin, direction)

    override val start: Double
        get() = this@offset.start + offset

    override val duration: Double
        get() = this@offset.duration
}