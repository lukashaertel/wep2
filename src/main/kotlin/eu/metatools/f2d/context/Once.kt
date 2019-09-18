package eu.metatools.f2d.context

import eu.metatools.f2d.math.CoordsAt

/**
 * Methods used to draw or play a subject to it's end or until the resulting [AutoCloseable] is invoked.
 */
class Once {

    interface Pending {
        val subject: Timed

        fun send(continuous: Continuous, time: Double)
    }

    /**
     * Represents a [Drawable] that will be rendered with a [Continuous].
     */
    private data class PendingDrawable<T>(
        override val subject: Drawable<T>,
        val args: T,
        val coordinates: CoordsAt
    ) : Pending {
        /**
         * Type safe delegate for draw in the [Continuous].
         */
        override fun send(continuous: Continuous, time: Double) {
            continuous.draw(time, subject, args, coordinates(time))
        }
    }

    /**
     * Represents a [Playable] that will be rendered with a [Continuous].
     */
    private data class PendingPlayable<T>(
        override val subject: Playable<T>,
        val key: Any,
        val args: T,
        val coordinates: CoordsAt
    ) : Pending {
        /**
         * Type safe delegate for play in the [Continuous].
         */
        override fun send(continuous: Continuous, time: Double) {
            continuous.play(time, subject, key, args, coordinates(time))
        }
    }

    /**
     * Represents a [Capturable] that will be rendered with a [Continuous].
     */
    private data class PendingCapturable<T>(
        val result: Any?,
        override val subject: Capturable<T>,
        val args: T,
        val coordinates: CoordsAt
    ) : Pending {
        /**
         * Type safe delegate for capture in the [Continuous].
         */
        override fun send(continuous: Continuous, time: Double) {
            continuous.capture(time, result, subject, args, coordinates(time))
        }
    }

    /**
     * Tracks all pending subjects.
     */
    private val pending = mutableSetOf<Pending>()

    /**
     * Adds a drawable [subject] with the given [coordinates] function to the queue, returns a method to remove it
     * before it's lifetime has ended.
     */
    fun <T> draw(subject: Drawable<T>, args: T, coordinates: CoordsAt): AutoCloseable {
        val element = PendingDrawable(subject, args, coordinates)
        pending.add(element)
        return AutoCloseable {
            pending.remove(element)
        }
    }


    /**
     * Auto-fills the nullable args with `null`.
     */
    fun <T> draw(subject: Drawable<T?>, coordinates: CoordsAt) =
        draw(subject, null, coordinates)

    /**
     * Adds a playable [subject] with the given [coordinates] function to the queue, returns a method to remove it
     * before it's lifetime has ended.
     */
    fun <T> play(subject: Playable<T>, args: T, coordinates: CoordsAt): AutoCloseable {
        val element = PendingPlayable(subject, Any(), args, coordinates)
        pending.add(element)
        return AutoCloseable {
            pending.remove(element)
        }
    }

    /**
     * Auto-fills the nullable args with `null`.
     */
    fun <T> play(subject: Playable<T?>, coordinates: CoordsAt) =
        play(subject, null, coordinates)


    /**
     * Adds a captureable [subject] with the given [coordinates] function to the queue, returns a method to remove it
     * before it's lifetime has ended.
     */
    fun <T> capture(result: Any?, subject: Capturable<T>, args: T, coordinates: CoordsAt): AutoCloseable {
        val element = PendingCapturable(result, subject, args, coordinates)
        pending.add(element)
        return AutoCloseable {
            pending.remove(element)
        }
    }

    /**
     * Auto-fills the nullable args with `null`.
     */
    fun <T> capture(result: Any?, subject: Capturable<T?>, coordinates: CoordsAt) =
        capture(result, subject, null, coordinates)

    /**
     * Sends all queued subjects to the [Continuous] context.
     */
    fun apply(continuous: Continuous, time: Double) {
        // Send or remove pending entries.
        pending.iterator().let {
            while (it.hasNext()) {
                val current = it.next()
                if (time < current.subject.start)
                    continue
                if (current.subject.end <= time)
                    it.remove()
                else
                    current.send(continuous, time)
            }
        }
    }
}