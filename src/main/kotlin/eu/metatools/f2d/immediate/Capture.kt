package eu.metatools.f2d.immediate

import eu.metatools.f2d.capturable.Capturable
import eu.metatools.f2d.data.Mat

/**
 * Directly captures a [Capturable].
 */
interface Capture {
    /**
     * Submits a capture call.
     */
    fun <T> submit(subject: Capturable<T>, args: T, result: Any, time: Double, transform: Mat)
}

/**
 * Submits a capture call with args set to `null`.
 */
fun <T> Capture.submit(subject: Capturable<T?>, result: Any, time: Double, transform: Mat) =
    submit(subject, null, result, time, transform)

abstract class TransformedCapture(val on: Capture) : Capture {
    abstract val mat: Mat
    override fun <T> submit(subject: Capturable<T>, args: T, result: Any, time: Double, transform: Mat) =
        on.submit(subject, args, result, time, mat * transform)
}