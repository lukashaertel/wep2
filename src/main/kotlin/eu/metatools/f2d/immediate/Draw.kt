package eu.metatools.f2d.immediate

import eu.metatools.f2d.data.Mat
import eu.metatools.f2d.drawable.Drawable

/**
 * Directly draws as [Drawable].
 */
interface Draw {

    /**
     * Submits a draw call.
     */
    fun <T> submit(subject: Drawable<T>, args: T, time: Double, transform: Mat)
}

/**
 * Submits a draw call with args set to `null`.
 */
fun <T> Draw.submit(subject: Drawable<T?>, time: Double, transform: Mat) =
    submit(subject, null, time, transform)

abstract class TransformedDraw(val on: Draw) : Draw {
    abstract val mat: Mat

    override fun <T> submit(subject: Drawable<T>, args: T, time: Double, transform: Mat) =
        on.submit(subject, args, time, mat * transform)
}