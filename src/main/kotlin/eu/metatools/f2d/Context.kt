package eu.metatools.f2d

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Matrix4

/**
 * Methods used to draw or play a subject to it's end or until the resulting [AutoCloseable] is invoked.
 */
class Once {
    /**
     * Tracks all open drawables.
     */
    private val drawable = mutableSetOf<Pair<Drawable, CoordsAt>>()

    /**
     * Tracks all open playables.
     */
    private val playable = mutableSetOf<Pair<Playable, CoordsAt>>()

    /**
     * Adds a drawable [subject] with the given [coordinates] function to the queue, returns a method to remove it
     * before it [Lifetime.hasEnded].
     */
    fun draw(subject: Drawable, coordinates: CoordsAt): AutoCloseable {
        val element = subject to coordinates
        drawable.add(element)
        return AutoCloseable {
            drawable.remove(element)
        }
    }

    /**
     * Adds a playable [subject] with the given [coordinates] function to the queue, returns a method to remove it
     * before it [Lifetime.hasEnded].
     */
    fun play(subject: Playable, coordinates: CoordsAt): AutoCloseable {
        val element = subject to coordinates
        playable.add(element)
        return AutoCloseable {
            playable.remove(element)
        }
    }

    /**
     * Sends all queued [Drawable]s and [Playable]s to the [Continuous] context.
     */
    fun send(continuous: Continuous, time: Double, world: Coords) {
        // Draw or remove drawable entries.
        drawable.iterator().let {
            while (it.hasNext()) {
                val (subject, coords) = it.next()
                if (subject.hasEnded(time))
                    it.remove()
                else
                    continuous.draw(time, world, subject, coords)
            }
        }

        // Play or remove playable entries.
        playable.iterator().let {
            while (it.hasNext()) {
                val (subject, coords) = it.next()
                if (subject.hasEnded(time))
                    it.remove()
                else
                    continuous.play(time, world, subject, coords)
            }
        }
    }
}

/**
 * Methods used to draw or play a subject continuously.
 */
class Continuous {
    /**
     * Combines a list of calls with the last assigned combined matrix.
     */
    data class ZEntry(var lastCombined: Coords?, val entries: MutableList<(SpriteBatch) -> Unit>)

    private val calls = sortedMapOf<Float, ZEntry>()

    fun draw(time: Double, world: Coords, subject: Drawable, coordinates: CoordsAt) {
        // Don't render subjects outside their lifetime.
        if (!subject.hasStarted(time) || subject.hasEnded(time))
            return

        // Create combined matrix.
        val combined = coordinates(time).mulLeft(world)

        // Read Z-value from the matrix.
        val z = combined.`val`[Matrix4.M32] / combined.`val`[Matrix4.M33]

        // Get the target for the given Z entry.
        val target = calls.getOrPut(z) { ZEntry(null, mutableListOf()) }

        // Add setting the combined matrix.
        if (target.lastCombined != combined) {
            target.lastCombined = combined
            target.entries.add {
                it.transformMatrix = combined
            }
        }

        // Generate calls for the subject.
        subject.generate(time) { target.entries.add(it) }
    }


    fun play(time: Double, world: Coords, subject: Playable, coordinates: CoordsAt) {
        // Don't play subjects outside their lifetime.
        if (!subject.hasStarted(time) || subject.hasEnded(time))
            return
    }

    fun send(spriteBatch: SpriteBatch) {
        // Render all entries for the Z-sorted set.
        calls.values.forEach { (_, entries) ->
            entries.forEach { it(spriteBatch) }
        }

        // Reset the call list.
        calls.clear()
    }
}