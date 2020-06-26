package eu.metatools.fio.queued

import eu.metatools.fio.data.Mat
import eu.metatools.fio.drawable.Drawable
import eu.metatools.fio.end
import eu.metatools.fio.immediate.Draw

class StandardQueuedDraw(val target: Draw) : QueuedDraw {
    private data class Entry<T>(
        val subject: Drawable<T>, val args: T, val transformAt: (Double) -> Mat
    ) {
        fun submit(target: Draw, time: Double) {
            target.submit(subject, args, time, transformAt(time))
        }
    }

    /**
     * Tracks all pending subjects.
     */
    private val pending = mutableSetOf<Entry<*>>()

    /**
     * Adds a captureable [subject] with the given [coordinates] function to the queue, returns a method to remove it
     * before it's lifetime has ended.
     */
    override fun <T> enqueue(subject: Drawable<T>, args: T, coordinates: (Double) -> Mat): AutoCloseable {
        val element = Entry(subject, args, coordinates)
        pending.add(element)
        return AutoCloseable {
            pending.remove(element)
        }
    }

    /**
     * Sends all queued subjects to the [StandardContinuous] context.
     */
    fun update(time: Double) {
        // Send or remove pending entries.
        pending.iterator().let {
            while (it.hasNext()) {
                val current = it.next()
                if (time < current.subject.start)
                    continue
                if (current.subject.end <= time)
                    it.remove()
                else
                    current.submit(target, time)
            }
        }
    }
}