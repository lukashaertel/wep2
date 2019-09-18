package eu.metatools.f2d.context

import eu.metatools.f2d.math.CoordsAt
import eu.metatools.f2d.math.Mat

/**
 * Methods used to draw or play a subject to it's end or until the resulting [AutoCloseable] is invoked.
 */
class Once {

    interface Pending {
        val subject: Timed

        fun submit(continuous: Continuous, time: Double)
    }

    /**
     * Capture call to be submitted to a [Continuous].
     */
    private data class Capture<T>(
        override val subject: Capturable<T>, val args: T, val result: Any, val transformAt: (Double) -> Mat
    ) : Pending {
        /**
         * Type safe delegate for capture in the [Continuous].
         */
        override fun submit(continuous: Continuous, time: Double) {
            continuous.submit(subject, args, result, time, transformAt(time))
        }
    }

    /**
     * Draw call to be submitted to a [Continuous].
     */
    private data class Draw<T>(
        override val subject: Drawable<T>, val args: T, val transformAt: (Double) -> Mat
    ) : Pending {
        override fun submit(continuous: Continuous, time: Double) {
            continuous.submit(subject, args, time, transformAt(time))
        }
    }


    /**
     * Play call to be submitted to a [Continuous].
     */
    private data class Play<T>(
        override val subject: Playable<T>, val args: T, val handle: Any, val transformAt: (Double) -> Mat
    ) : Pending {
        override fun submit(continuous: Continuous, time: Double) {
            continuous.submit(subject, args, handle, time, transformAt(time))
        }
    }

    /**
     * Tracks all pending subjects.
     */
    private val pending = mutableSetOf<Pending>()

    /**
     * Adds a captureable [subject] with the given [coordinates] function to the queue, returns a method to remove it
     * before it's lifetime has ended.
     */
    fun <T> enqueue(subject: Capturable<T>, args: T, result: Any, coordinates: CoordsAt): AutoCloseable {
        val element = Capture(subject, args, result, coordinates)
        pending.add(element)
        return AutoCloseable {
            pending.remove(element)
        }
    }

    /**
     * Auto-fills the nullable args with `null`.
     */
    fun <T> enqueue(subject: Capturable<T?>, result: Any, coordinates: CoordsAt) =
        enqueue(subject, null, result, coordinates)

    /**
     * Adds a drawable [subject] with the given [coordinates] function to the queue, returns a method to remove it
     * before it's lifetime has ended.
     */
    fun <T> enqueue(subject: Drawable<T>, args: T, coordinates: CoordsAt): AutoCloseable {
        val element = Draw(subject, args, coordinates)
        pending.add(element)
        return AutoCloseable {
            pending.remove(element)
        }
    }


    /**
     * Auto-fills the nullable args with `null`.
     */
    fun <T> enqueue(subject: Drawable<T?>, coordinates: CoordsAt) =
        enqueue(subject, null, coordinates)

    /**
     * Adds a playable [subject] with the given [coordinates] function to the queue, returns a method to remove it
     * before it's lifetime has ended.
     */
    fun <T> enqueue(subject: Playable<T>, args: T, coordinates: CoordsAt): AutoCloseable {
        val element = Play(subject, args, Any(), coordinates)
        pending.add(element)
        return AutoCloseable {
            pending.remove(element)
        }
    }

    /**
     * Auto-fills the nullable args with `null`.
     */
    fun <T> enqueue(subject: Playable<T?>, coordinates: CoordsAt) =
        enqueue(subject, null, coordinates)


    /**
     * Sends all queued subjects to the [Continuous] context.
     */
    fun send(continuous: Continuous, time: Double) {
        // Send or remove pending entries.
        pending.iterator().let {
            while (it.hasNext()) {
                val current = it.next()
                if (time < current.subject.start)
                    continue
                if (current.subject.end <= time)
                    it.remove()
                else
                    current.submit(continuous, time)
            }
        }
    }
}