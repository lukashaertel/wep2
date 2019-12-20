package eu.metatools.f2d.context

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import eu.metatools.f2d.math.Mat
import eu.metatools.f2d.math.Vec
import eu.metatools.f2d.math.Vecs
import eu.metatools.f2d.math.reduceComponents
import java.util.*

/**
 * Standard implementation of [Continuous].
 * @property trimExcess How far outside of the view a center must lie for the subject to be ignored.
 */
class StandardContinuous(val trimExcess: Float = 0.25f) : Continuous {

    /**
     * Capture in the current [begin]/[end] block.
     */
    private data class Capture<T>(
        val subject: Capturable<T>, val args: T, val result: Any, val transform: Mat
    ) {
        fun capture(time: Double, origin: Vec, direction: Vec) =
            // Capture in local system.
            subject.capture(args, time, transform.inv * origin, transform.inv.rotate(direction))?.let {
                // Invert intersection if valid.
                transform * it
            }
    }

    /**
     * Draw in the current [begin]/[end] block.
     */
    private data class Draw<T>(
        val subject: Drawable<T>, val args: T, val transform: Mat
    ) {
        fun draw(time: Double, spriteBatch: SpriteBatch) {
            // Override transform.
            spriteBatch.transformMatrix = spriteBatch.transformMatrix.set(transform.values)

            // Draw subject itself.
            subject.draw(args, time, spriteBatch)
        }
    }

    /**
     * Play in the current [begin]/[end] block.
     */
    private data class Play<T>(val subject: Playable<T>, val args: T, val handle: Any) {
        fun play(time: Double, transform: Mat) {
            // Play subject itself.
            subject.play(args, handle, time, transform)
        }

        fun cancel() {
            // Cancel the handle for the subject.
            subject.cancel(handle)
        }
    }

    /**
     * Vectors that are [trimExcess] outside the uniform projection space.
     */
    private val excessVectors = Vecs(8) {
        Vec(
            if ((it / 4) % 2 == 0) -1f - trimExcess else 1f + trimExcess,
            if ((it / 2) % 2 == 0) -1f - trimExcess else 1f + trimExcess,
            if ((it / 1) % 2 == 0) -1f - trimExcess else 1f + trimExcess
        )
    }

    /**
     * The projection matrix in the current block.
     */
    private var projection = Mat.NAN

    /**
     * The minimum in-bounds vector.
     */
    private var excessMin = Vec.NaN

    /**
     * The maximum in-bounds vector.
     */
    private var excessMax = Vec.NaN

    /**
     * All draws in the current block.
     */
    private val draws = hashMapOf<Float, MutableList<Draw<*>>>()

    /**
     * All captures in the current block.
     */
    private val captures = hashMapOf<Float, MutableList<Capture<*>>>()

    /**
     * All plays in the current block, differs in implementation, as sounds are updated from continuous, while
     * captures and draws are always passed through. Therefore an identity is needed, i.e., what to play, what
     * argument and what handle. The value of this association is the transformation to update to.
     */
    private var plays = hashMapOf<Play<*>, Mat>()

    /**
     * Plays from the last block.
     */
    private var playsSecondary = hashMapOf<Play<*>, Mat>()

    /**
     * Starts the block with the given matrices.
     */
    fun begin(projection: Mat) {
        // Transfer matrices.
        this.projection = projection

        // Setup bounds.
        excessMin = Vec.PositiveInfinity
        excessMax = Vec.NegativeInfinity
        for (v in projection.inv * excessVectors) {
            excessMin = reduceComponents(excessMin, v, ::minOf)
            excessMax = reduceComponents(excessMax, v, ::maxOf)
        }

        // Prepare buffers.
        draws.clear()
        captures.clear()
        plays.clear()
    }

    /**
     * Plays all play calls in this block.
     */
    fun play(time: Double) {
        // Play all collected entries.
        for ((play, transform) in plays)
            play.play(time, transform)
    }

    /**
     * Renders all draw calls in this block.
     */
    fun render(time: Double, spriteBatch: SpriteBatch) {
        // Memorize previous values.
        val transformBefore = spriteBatch.transformMatrix.cpy()
        val projectionBefore = spriteBatch.projectionMatrix.cpy()

        // Set projection, begin batch. Transform will be set by draws itself.
        spriteBatch.projectionMatrix = projectionBefore.set(projection.values)
        spriteBatch.begin()

        // Create tree map from draws.
        val sortedDraws = TreeMap(draws).descendingMap()

        // Iterate all Z-layers and all draw calls.
        for ((_, draws) in sortedDraws)
            for (draw in draws)
                draw.draw(time, spriteBatch)

        // End batch, rest model and projection.
        spriteBatch.end()
        spriteBatch.transformMatrix = transformBefore
        spriteBatch.projectionMatrix = projectionBefore
    }

    /**
     * Captures input on negative Z axis and projection space coordinates [x] and [y].
     */
    fun collect(time: Double, x: Float, y: Float): Pair<Any?, Vec> {
        // Create ray in inverted projection space.
        val origin = projection.inv * Vec(x, y, 1f)
        val direction = projection.inv.rotate(-Vec.Z)

        // Create tree map from captures.
        val sortedCaptures = TreeMap(captures).descendingMap()

        // Iterate all Z-layers and all capture calls.
        for ((_, captures) in sortedCaptures)
            for (capture in captures) {
                // Try to capture.
                val intersection = capture.capture(time, origin, direction)

                // Successfully captured, return result paired with intersection point.
                if (intersection != null)
                    return capture.result to intersection
            }

        return null to origin
    }

    /**
     * Resets the model and projection matrices, cancels all non-renewed sounds.
     */
    fun end() {
        // Reset bounds and matrices.
        projection = Mat.NAN
        excessMin = Vec.NaN
        excessMax = Vec.NaN

        // Cancel previous plays that were not renewed.
        for (play in playsSecondary.keys subtract plays.keys)
            play.cancel()

        // Swap plays.
        plays.let {
            plays = playsSecondary
            playsSecondary = it
        }
    }

    /**
     * Validates time, does not perform boundary check.
     */
    private inline fun validate(timed: Timed, time: Double, block: () -> Unit) {
        // Not within time frame, return.
        if (time < timed.start) return
        if (timed.end <= time) return

        // Valid, run block.
        block()
    }

    /**
     * Validates time and performs boundary check.
     */
    private inline fun validate(timed: Timed, time: Double, transform: Mat, block: () -> Unit) {
        // Not within time frame, return.
        if (time < timed.start) return
        if (timed.end <= time) return

        // Not within visible portion, return.
        transform.center.let { (x, y, z) ->
            if (x < excessMin.x) return
            if (y < excessMin.y) return
            if (z < excessMin.z) return
            if (excessMax.x < x) return
            if (excessMax.y < y) return
            if (excessMax.z < z) return
        }

        // Valid, run block.
        block()
    }

    /**
     * Submits a capture call.
     */
    override fun <T> submit(subject: Capturable<T>, args: T, result: Any, time: Double, transform: Mat) {
        // Validate time and bounds.
        validate(subject, time, transform) {
            // Add a capture call on the correct Z index.
            captures.getOrPut(transform.center.z, ::mutableListOf).add(
                Capture(subject, args, result, transform)
            )
        }
    }

    /**
     * Submits a capture call with args set to `null`.
     */
    override fun <T> submit(subject: Capturable<T?>, result: Any, time: Double, transform: Mat) =
        submit(subject, null, result, time, transform)

    /**
     * Submits a draw call.
     */
    override fun <T> submit(subject: Drawable<T>, args: T, time: Double, transform: Mat) {
        // Validate time and bounds.
        validate(subject, time, transform) {
            // Add a draw call on the correct Z index.
            draws.getOrPut(transform.center.z, ::mutableListOf).add(
                Draw(subject, args, transform)
            )
        }
    }

    /**
     * Submits a draw call with args set to `null`.
     */
    override fun <T> submit(subject: Drawable<T?>, time: Double, transform: Mat) =
        submit(subject, null, time, transform)

    /**
     * Submits a play call.
     */
    override fun <T> submit(subject: Playable<T>, args: T, handle: Any, time: Double, transform: Mat) {
        // Validate time (no bounds for sound).
        validate(subject, time) {
            // Add to plays, sound is played in active projection space.
            plays[Play(subject, args, handle)] =
                projection * transform
        }
    }

    /**
     * Submits a play call with args set to `null`.
     */
    override fun <T> submit(subject: Playable<T?>, handle: Any, time: Double, transform: Mat) =
        submit(subject, null, handle, time, transform)
}