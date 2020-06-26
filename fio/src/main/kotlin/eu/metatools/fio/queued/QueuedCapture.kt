package eu.metatools.fio.queued

import eu.metatools.fio.capturable.Capturable
import eu.metatools.fio.data.Mat

interface QueuedCapture {
    /**
     * Adds a captureable [subject] with the given [coordinates] function to the queue, returns a method to remove it
     * before it's lifetime has ended.
     */
    fun <T> enqueue(subject: Capturable<T>, args: T, result: Any, coordinates: (Double) -> Mat): AutoCloseable
}

/**
 * Auto-fills the nullable args with `null`.
 */
fun <T> QueuedCapture.enqueue(subject: Capturable<T?>, result: Any, coordinates: (Double) -> Mat) =
    enqueue(subject, null, result, coordinates)