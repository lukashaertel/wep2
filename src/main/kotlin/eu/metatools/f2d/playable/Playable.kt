package eu.metatools.f2d.playable

import eu.metatools.f2d.Timed
import eu.metatools.f2d.data.Mat

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
