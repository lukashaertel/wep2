package eu.metatools.f2d.context

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.Ray
import eu.metatools.f2d.math.Coords
import java.util.*

/**
 * Methods used to draw or play a subject continuously.
 */
class Continuous {

    /**
     * All Z-sorted calls performing visualization in the sprite-batch.
     */
    private val drawRows = TreeMap<Float, MutableList<(SpriteBatch) -> Unit>>()

    private val captureRows = TreeMap<Float, MutableList<Pair<Any?, (Ray, Vector3) -> Boolean>>>()

    private var minX: Float = Float.NEGATIVE_INFINITY
    private var minY: Float = Float.NEGATIVE_INFINITY
    private var minZ: Float = Float.NEGATIVE_INFINITY
    private var maxX: Float = Float.POSITIVE_INFINITY
    private var maxY: Float = Float.POSITIVE_INFINITY
    private var maxZ: Float = Float.POSITIVE_INFINITY

    /**
     * Draws a visible instance [subject]  with the [time] and the [coordinates]. Passes the [args].
     */
    fun <T> draw(time: Double, subject: Drawable<T>, args: T, coords: Coords) {
        // Don't render subjects outside their lifetime.
        if (time < subject.start || subject.end <= time)
            return

        // Read center from the matrix, abort any outside of bounds.
        val x = coords.`val`[Matrix4.M03] / coords.`val`[Matrix4.M33]
        if (x < minX || maxX < x) return

        val y = coords.`val`[Matrix4.M13] / coords.`val`[Matrix4.M33]
        if (y < minY || maxY < y) return

        val z = coords.`val`[Matrix4.M23] / coords.`val`[Matrix4.M33]
        if (z < minZ || maxZ < z) return

        // Get the target for the given Z entry.
        val target = drawRows.getOrPut(z, ::mutableListOf)

        // Add setting the combined matrix.
        target.add {
            it.transformMatrix = coords
        }

        // Generate calls for the subject.
        subject.draw(args, time) { target.add(it) }
    }

    /**
     * Auto-fills the nullable args with `null`.
     */
    fun <T> draw(time: Double, subject: Drawable<T?>, transform: Coords) =
        draw(time, subject, null, transform)

    fun <T> capture(time: Double, result: Any?, subject: Capturable<T>, args: T, coords: Coords) {
        // Don't render subjects outside their lifetime.
        if (time < subject.start || subject.end <= time)
            return

        // Read center from the matrix, abort any outside of bounds.
        val x = coords.`val`[Matrix4.M03] / coords.`val`[Matrix4.M33]
        if (x < minX || maxX < x) return

        val y = coords.`val`[Matrix4.M13] / coords.`val`[Matrix4.M33]
        if (y < minY || maxY < y) return

        val z = coords.`val`[Matrix4.M23] / coords.`val`[Matrix4.M33]
        if (z < minZ || maxZ < z) return

        // Invert transformatuion.
        val inverse = coords.cpy().inv()

        // Get the target for the given Z entry.
        val target = captureRows.getOrPut(z, ::mutableListOf)

        // Generate calls for the subject.
        subject.capture(args, time) { f ->
            target.add(result to { ray, intersection ->
                // Multiply a copy of the ray in the inverted coordinate system, creating
                // a uniform representation.
                f(ray.cpy().mul(inverse), intersection).also {
                    // If actually intersected, transform the uniform coordinate back into the world coordinate.
                    if (it)
                        intersection.mul(coords)
                }
            })
        }
    }

    fun <T> capture(time: Double, result: Any?, subject: Capturable<T?>, transform: Coords) =
        capture(time, result, subject, null, transform)

    private val playLast = mutableMapOf<Playable<*>, MutableSet<Any>>()
    private val playCurrent = mutableMapOf<Playable<*>, MutableSet<Any>>()

    /**
     * Plays or keeps playing a sound instance [subject] for the given [handle] with the [time] and the
     * listener-relative [coordinates]. Passes the [args].
     */
    fun <T> play(time: Double, subject: Playable<T>, handle: Any, args: T, coords: Coords) {
        // Don't play subjects outside their lifetime.
        if (time < subject.start || subject.end <= time)
            return

        // Read center from the matrix.
        val x = coords.`val`[Matrix4.M03] / coords.`val`[Matrix4.M33]
        val y = coords.`val`[Matrix4.M13] / coords.`val`[Matrix4.M33]
        val z = coords.`val`[Matrix4.M23] / coords.`val`[Matrix4.M33]

        // Remove from last play.
        playLast[subject]?.let { if (it.remove(handle) && it.isEmpty()) playLast.remove(subject) }

        // Add to current play.
        playCurrent.getOrPut(subject, ::mutableSetOf).add(handle)

        // Upload sound with the given handle
        subject.play(args, handle, time, x, y, z)
    }

    /**
     * Auto-fills the nullable args with `null`.
     */
    fun <T> play(time: Double, subject: Playable<T?>, handle: Any, coords: Coords) =
        play(time, subject, handle, null, coords)

    /**
     * Computes boundary points from the given projection matrix.
     * The [excess] specified how much a position outside of the view frustum may be without being skipped.
     */
    fun computeBounds(projectionMatrix: Matrix4, excess: Float = 0.25f) {
        val edgePoints = listOf(-1f - excess, 1f + excess)

        minX = Float.POSITIVE_INFINITY
        minY = Float.POSITIVE_INFINITY
        minZ = Float.POSITIVE_INFINITY
        maxX = Float.NEGATIVE_INFINITY
        maxY = Float.NEGATIVE_INFINITY
        maxZ = Float.NEGATIVE_INFINITY

        val inverse = projectionMatrix.cpy().inv()
        for (x in edgePoints) for (y in edgePoints) for (z in edgePoints) {
            val xyz = Vector3(x, y, z).mul(inverse)
            minX = minOf(minX, xyz.x)
            minY = minOf(minY, xyz.y)
            minZ = minOf(minZ, xyz.z)
            maxX = maxOf(maxX, xyz.x)
            maxY = maxOf(maxY, xyz.y)
            maxZ = maxOf(maxZ, xyz.z)
        }
    }

    /**
     * Resets the bound to be unbounded.
     */
    fun resetBounds() {
        minX = Float.NEGATIVE_INFINITY
        minY = Float.NEGATIVE_INFINITY
        minZ = Float.NEGATIVE_INFINITY
        maxX = Float.POSITIVE_INFINITY
        maxY = Float.POSITIVE_INFINITY
        maxZ = Float.POSITIVE_INFINITY
    }

    /**
     * Collects the first hit in the captures, returns the associated result and the intersection vector.
     */
    fun collect(projectionMatrix: Matrix4, position: Vector2): Pair<Any?, Vector3>? {
        // Create ray in inverted projection space.
        val inverse = projectionMatrix.cpy().inv()
        val ray = Ray(Vector3(position.x, position.y, 1f), Vector3(0f, 0f, -1f))
        ray.mul(inverse)

        // Create output vector.
        val intersection = Vector3()

        // Iterate inverse Z-sorting.
        captureRows.descendingMap().values.forEach {
            it.forEach { (r, f) ->
                // If there is an intersection, return it.
                if (f(ray, intersection))
                    return r to intersection
            }
        }

        return null
    }

    /**
     * Sends all the calls to the sprite batch, clear the capture buffers, stops untouched continuous sounds.
     */
    fun apply(spriteBatch: SpriteBatch) {
        // Remember previous transformation.
        val previousTransform = spriteBatch.transformMatrix.cpy()

        // Render all entries for the Z-sorted set (lower Z-values being rendered later).
        drawRows.descendingMap().values.forEach {
            it.forEach {
                it(spriteBatch)
            }
        }

        // Reset transformation.
        spriteBatch.transformMatrix = previousTransform

        // Cancel outdated instances.
        playLast.forEach { (p, hs) ->
            hs.forEach { h ->
                p.cancel(h)
            }
        }

        // Clear outdated set.
        playLast.putAll(playCurrent)
        playCurrent.clear()

        // Clear buffers.
        drawRows.clear()
        captureRows.clear()
    }
}
