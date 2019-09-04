package eu.metatools.f2d.context

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Matrix4
import eu.metatools.f2d.Coords
import eu.metatools.f2d.CoordsAt

/**
 * Methods used to draw or play a subject to it's end or until the resulting [AutoCloseable] is invoked.
 */
class Once {
    /**
     * Represents a [Drawable] that will be rendered with a [Continuous].
     */
    private data class PendingDrawable<T>(
        val subject: Drawable<T>,
        val args: T,
        val coordinates: CoordsAt
    ) {
        /**
         * Type safe delegate for lifetime check.
         */
        fun hasStarted(time: Double) =
            subject.hasStarted(args, time)

        /**
         * Type safe delegate for lifetime check.
         */
        fun hasEnded(time: Double) =
            subject.hasEnded(args, time)

        /**
         * Type safe delegate for draw in the [Continuous].
         */
        fun render(continuous: Continuous, time: Double) {
            continuous.draw(time, subject, args, coordinates)
        }
    }

    /**
     * Represents a [Playable] that will be rendered with a [Continuous].
     */
    private data class PendingPlayable<T>(
        val key: Any,
        val subject: Playable<T>,
        val args: T,
        val coordinates: CoordsAt
    ) {
        /**
         * Type safe delegate for lifetime check.
         */
        fun hasStarted(time: Double) =
            subject.hasStarted(args, time)

        /**
         * Type safe delegate for lifetime check.
         */
        fun hasEnded(time: Double) =
            subject.hasEnded(args, time)

        /**
         * Type safe delegate for play in the [Continuous].
         */
        fun render(continuous: Continuous, time: Double) {
            continuous.play(key, time, subject, args, coordinates)
        }
    }

    /**
     * Tracks all open drawables.
     */
    private val drawable = mutableSetOf<PendingDrawable<*>>()

    /**
     * Tracks all open playables.
     */
    private val playable = mutableSetOf<PendingPlayable<*>>()

    /**
     * Adds a drawable [subject] with the given [coordinates] function to the queue, returns a method to remove it
     * before it [Lifetime.hasEnded].
     */
    fun <T> draw(subject: Drawable<T>, args: T, coordinates: CoordsAt): AutoCloseable {
        val element = PendingDrawable(subject, args, coordinates)
        drawable.add(element)
        return AutoCloseable {
            drawable.remove(element)
        }
    }

    /**
     * Auto-fills the args with [Unit].
     */
    fun draw(subject: Drawable<Unit>, coordinates: CoordsAt) =
        draw(subject, Unit, coordinates)

    /**
     * Adds a playable [subject] with the given [coordinates] function to the queue, returns a method to remove it
     * before it [Lifetime.hasEnded].
     */
    fun <T> play(subject: Playable<T>, args: T, coordinates: CoordsAt): AutoCloseable {
        val element = PendingPlayable(Any(), subject, args, coordinates)
        playable.add(element)
        return AutoCloseable {
            playable.remove(element)
        }
    }

    /**
     * Auto-fills the args with [Unit].
     */
    fun play(subject: Playable<Unit>, coordinates: CoordsAt) =
        play(subject, Unit, coordinates)

    /**
     * Sends all queued [Drawable]s and [Playable]s to the [Continuous] context.
     */
    fun dispatch(continuous: Continuous, time: Double) {
        // Draw or remove drawable entries.
        drawable.iterator().let {
            while (it.hasNext()) {
                val current = it.next()
                if (!current.hasStarted(time))
                    continue
                if (current.hasEnded(time))
                    it.remove()
                else
                    current.render(continuous, time)
            }
        }

        // Play or remove playable entries.
        playable.iterator().let {
            while (it.hasNext()) {
                val current = it.next()
                if (!current.hasStarted(time))
                    continue
                if (current.hasEnded(time))
                    it.remove()
                else
                    current.render(continuous, time)
            }
        }
    }
}

/**
 * Auto-fills the nullable args with `null`.
 */
@JvmName("drawNullArg")
fun <T> Once.draw(subject: Drawable<T?>, coordinates: CoordsAt) =
    this.draw(subject, null, coordinates)

/**
 * Auto-fills the nullable args with `null`.
 */
@JvmName("playNullArg")
fun <T> Once.play(subject: Playable<T?>, coordinates: CoordsAt) =
    this.play(subject, null, coordinates)

/**
 * Methods used to draw or play a subject continuously.
 */
class Continuous {
    /**
     * Combines a list of calls with the last assigned combined matrix.
     */
    data class ZEntry(var lastCombined: Coords?, val entries: MutableList<(SpriteBatch) -> Unit>)

    /**
     * All Z-sorted calls performing visualization in the sprite-batch.
     */
    private val calls = sortedMapOf<Float, ZEntry>()

    /**
     * Draws a visible instance [subject]  with the [time] and the [coordinates]. Passes the [args].
     */
    fun <T> draw(time: Double, subject: Drawable<T>, args: T, coordinates: CoordsAt) {
        // Don't render subjects outside their lifetime.
        if (!subject.hasStarted(args, time) || subject.hasEnded(args, time))
            return

        // Create combined matrix.
        val coords = coordinates(time)

        // Read Z-value from the matrix.
        val z = coords.`val`[Matrix4.M23] / coords.`val`[Matrix4.M33]

        // Get the target for the given Z entry.
        val target = calls.getOrPut(z) { ZEntry(null, mutableListOf()) }

        // Add setting the combined matrix.
        if (target.lastCombined != coords) {
            target.lastCombined = coords
            target.entries.add {
                it.transformMatrix = coords
            }
        }

        // Generate calls for the subject.
        subject.generate(args, time) { target.entries.add(it) }
    }

    fun draw(time: Double, subject: Drawable<Unit>, coordinates: CoordsAt) =
        draw(time, subject, Unit, coordinates)

    /**
     * Represents an active [Playable] with it's arguments.
     */
    private data class ActivePlayable<T>(val key: Any, val subject: Playable<T>, val args: T) {
        /**
         * Type safe delegate to stopping the playable with arguments.
         */
        fun stop(id: Long) {
            subject.stop(args, id)
        }
    }

    /**
     * All sounds that are active, mapped to their sound identities.
     */
    private val sounds = mutableMapOf<ActivePlayable<*>, Long>()

    /**
     * All sounds that were touched between two calls to [render]. Untouched entries are stopped on calling [render].
     */
    private val soundsRenewed = mutableSetOf<ActivePlayable<*>>()

    /**
     * Plays or keeps playing a sound instance [subject] for the given [key] with the [time] and the
     * listener-relative [coordinates]. Passes the [args].
     */
    fun <T> play(key: Any, time: Double, subject: Playable<T>, args: T, coordinates: CoordsAt) {
        // Don't play subjects outside their lifetime.
        if (!subject.hasStarted(args, time) || subject.hasEnded(args, time))
            return

        // Create combined matrix.
        val combined = coordinates(time)

        // Read center from the matrix.
        val x = combined.`val`[Matrix4.M03] / combined.`val`[Matrix4.M33]
        val y = combined.`val`[Matrix4.M13] / combined.`val`[Matrix4.M33]
        val z = combined.`val`[Matrix4.M23] / combined.`val`[Matrix4.M33]

        // Compute active sound key.
        val active = ActivePlayable(key, subject, args)

        // Mark key as touched so that it is not collected.
        soundsRenewed.add(active)

        // Get ID of the sound or start playing a new one.
        val id = sounds.getOrPut(active) { subject.start(args, time) }
        subject.generate(args, id, time, x, y, z)
    }

    fun play(key: Any, time: Double, subject: Playable<Unit>, coordinates: CoordsAt) =
        play(key, time, subject, Unit, coordinates)

    /**
     * Sends the cached calls to the sprite batch, updates outdated sounds.
     */
    fun render(spriteBatch: SpriteBatch) {
        // Render all entries for the Z-sorted set.
        calls.values.forEach { (_, entries) ->
            entries.forEach { it(spriteBatch) }
        }

        // Reset the call list.
        calls.clear()

        // Stop all sounds that have not been renewed in this frame.
        sounds.iterator().let {
            while (it.hasNext()) {
                val (key, id) = it.next()
                if (key !in soundsRenewed) {
                    key.stop(id)
                    it.remove()
                }
            }
        }

        // Clear touched sounds.
        soundsRenewed.clear()
    }
}

/**
 * Auto-fills the nullable args with `null`.
 */
@JvmName("drawNullArg")
fun <T> Continuous.draw(time: Double, subject: Drawable<T?>, coordinates: CoordsAt) =
    this.draw(time, subject, null, coordinates)

/**
 * Auto-fills the nullable args with `null`.
 */
@JvmName("playNullArg")
fun <T> Continuous.play(key: Any, time: Double, subject: Playable<T?>, coordinates: CoordsAt) =
    this.play(key, time, subject, null, coordinates)