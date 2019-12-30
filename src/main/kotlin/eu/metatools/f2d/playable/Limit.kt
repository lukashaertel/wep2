package eu.metatools.f2d.playable

import eu.metatools.f2d.data.Mat

/**
 * Returns a playable instance that is fixed to end after the given time.
 */
infix fun <T> Playable<T>.limit(duration: Double) = object :
    Playable<T> {
    override fun play(args: T, handle: Any, time: Double, transform: Mat) =
        this@limit.play(args, handle, time, transform)

    override fun cancel(handle: Any) =
        this@limit.cancel(handle)

    override val start: Double
        get() = this@limit.start

    override val duration: Double
        get() = minOf(this@limit.duration, duration)
}