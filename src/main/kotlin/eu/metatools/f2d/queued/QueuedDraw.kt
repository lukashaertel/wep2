package eu.metatools.f2d.queued

import eu.metatools.f2d.data.Mat
import eu.metatools.f2d.drawable.Drawable

interface QueuedDraw {
    /**
     * Adds a drawable [subject] with the given [coordinates] function to the queue, returns a method to remove it
     * before it's lifetime has ended.
     */
    fun <T> enqueue(subject: Drawable<T>, args: T, coordinates: (Double) -> Mat): AutoCloseable
}

/**
 * Auto-fills the nullable args with `null`.
 */
fun <T> QueuedDraw.enqueue(subject: Drawable<T?>, coordinates: (Double) -> Mat) =
    enqueue(subject, null, coordinates)