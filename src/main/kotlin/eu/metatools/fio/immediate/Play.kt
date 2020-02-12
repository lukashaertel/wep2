package eu.metatools.fio.immediate

import eu.metatools.fio.data.Mat
import eu.metatools.fio.playable.Playable

/**
 * Directly plays a [Playable].
 */
interface Play {
    /**
     * Submits a play call.
     */
    fun <T> submit(subject: Playable<T>, args: T, handle: Any, time: Double, transform: Mat)
}

/**
 * Submits a play call with args set to `null`.
 */
fun <T> Play.submit(subject: Playable<T?>, handle: Any, time: Double, transform: Mat) =
    submit(subject, null, handle, time, transform)

/**
 * Transforms all inputs to the [Play] with [mat].
 */
fun Play.transform(mat: Mat) = object :
    Play {
    override fun <T> submit(subject: Playable<T>, args: T, handle: Any, time: Double, transform: Mat) =
        this@transform.submit(subject, args, handle, time, mat * transform)
}

abstract class TransformedPlay(val on: Play) : Play {
    abstract val mat: Mat

    override fun <T> submit(subject: Playable<T>, args: T, handle: Any, time: Double, transform: Mat) =
        on.submit(subject, args, handle, time, mat * transform)
}