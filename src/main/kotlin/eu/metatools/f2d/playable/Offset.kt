package eu.metatools.f2d.playable

import eu.metatools.f2d.data.Mat

/**
 * Returns a playable instance that is offset by the given time.
 */
infix fun <T> Playable<T>.offset(offset: Double) = object :
    Playable<T> {
    override fun play(args: T, handle: Any, time: Double, transform: Mat) =
        this@offset.play(args, handle, time - offset, transform)

    override fun cancel(handle: Any) =
        this@offset.cancel(handle)

    override val start: Double
        get() = this@offset.start + offset

    override val duration: Double
        get() = this@offset.duration
}