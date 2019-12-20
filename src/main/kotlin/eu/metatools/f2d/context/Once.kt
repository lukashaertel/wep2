package eu.metatools.f2d.context

import eu.metatools.f2d.math.CoordsAt

/**
 * Methods used to draw or play a subject to it's end or until the resulting [AutoCloseable] is invoked.
 */
interface Once {
    /**
     * Adds a captureable [subject] with the given [coordinates] function to the queue, returns a method to remove it
     * before it's lifetime has ended.
     */
    fun <T> enqueue(subject: Capturable<T>, args: T, result: Any, coordinates: CoordsAt): AutoCloseable

    /**
     * Auto-fills the nullable args with `null`.
     */
    fun <T> enqueue(subject: Capturable<T?>, result: Any, coordinates: CoordsAt): AutoCloseable

    /**
     * Adds a drawable [subject] with the given [coordinates] function to the queue, returns a method to remove it
     * before it's lifetime has ended.
     */
    fun <T> enqueue(subject: Drawable<T>, args: T, coordinates: CoordsAt): AutoCloseable

    /**
     * Auto-fills the nullable args with `null`.
     */
    fun <T> enqueue(subject: Drawable<T?>, coordinates: CoordsAt): AutoCloseable

    /**
     * Adds a playable [subject] with the given [coordinates] function to the queue, returns a method to remove it
     * before it's lifetime has ended.
     */
    fun <T> enqueue(subject: Playable<T>, args: T, coordinates: CoordsAt): AutoCloseable

    /**
     * Auto-fills the nullable args with `null`.
     */
    fun <T> enqueue(subject: Playable<T?>, coordinates: CoordsAt): AutoCloseable
}