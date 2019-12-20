package eu.metatools.f2d.context

import eu.metatools.f2d.math.Mat

/**
 * Methods to draw, play sounds and to capture input.
 */
interface Continuous {
    /**
     * Submits a capture call.
     */
    fun <T> submit(subject: Capturable<T>, args: T, result: Any, time: Double, transform: Mat)

    /**
     * Submits a capture call with args set to `null`.
     */
    fun <T> submit(subject: Capturable<T?>, result: Any, time: Double, transform: Mat)

    /**
     * Submits a draw call.
     */
    fun <T> submit(subject: Drawable<T>, args: T, time: Double, transform: Mat)

    /**
     * Submits a draw call with args set to `null`.
     */
    fun <T> submit(subject: Drawable<T?>, time: Double, transform: Mat)

    /**
     * Submits a play call.
     */
    fun <T> submit(subject: Playable<T>, args: T, handle: Any, time: Double, transform: Mat)

    /**
     * Submits a play call with args set to `null`.
     */
    fun <T> submit(subject: Playable<T?>, handle: Any, time: Double, transform: Mat)
}