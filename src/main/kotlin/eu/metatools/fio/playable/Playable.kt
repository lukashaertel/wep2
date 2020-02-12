package eu.metatools.fio.playable

import eu.metatools.fio.Timed
import eu.metatools.fio.data.Mat

/**
 * A playable instance.
 */
interface Playable<in T> : Timed {
    /**
     * Starts or updates the instance with the given [handle].
     */
    fun play(args: T, handle: Any, time: Double, transform: Mat)

    /**
     * Cancels the instance with the given internal handle.
     */
    fun cancel(handle: Any)
}
