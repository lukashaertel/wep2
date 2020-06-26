package eu.metatools.fio.queued

import eu.metatools.fio.data.Mat
import eu.metatools.fio.playable.Playable

interface QueuedPlay {
    /**
     * Adds a playable [subject] with the given [coordinates] function to the queue, returns a method to remove it
     * before it's lifetime has ended.
     */
    fun <T> enqueue(subject: Playable<T>, args: T, coordinates: (Double) -> Mat): AutoCloseable

}

/**
 * Auto-fills the nullable args with `null`.
 */
fun <T> QueuedPlay.enqueue(subject: Playable<T?>, coordinates: (Double) -> Mat) =
    enqueue(subject, null, coordinates)