package eu.metatools.fio.immediate

import eu.metatools.fio.capturable.Capturable
import eu.metatools.fio.end
import eu.metatools.fio.data.Mat
import eu.metatools.fio.data.Vec
import java.util.*

/**
 * Standard implementation of [Immediate].
 * @property trimExcess How far outside of the view a center must lie for the subject to be ignored.
 */
class StandardCapture(trimExcess: Float ) : ProjectionTrimmed(trimExcess), Capture {
    /**
     * Capture in the current [begin]/[end] block.
     */
    private data class Entry<T>(val subject: Capturable<T>, val args: T, val result: Any, val transform: Mat) {
        fun capture(time: Double, origin: Vec, direction: Vec) =
            // Capture in local system.
            subject.capture(args, time, transform.inv * origin, transform.inv.rotate(direction))?.let {
                // Invert intersection if valid.
                transform * it
            }
    }

    /**
     * All captures in the current block.
     */
    private val captures = hashMapOf<Float, MutableList<Entry<*>>>()

    /**
     * Starts the block with the given matrices.
     */
    fun begin(projection: Mat) {
        this.projection = projection
    }

    /**
     * Captures input on negative Z axis and projection space coordinates [x] and [y].
     */
    fun collect(time: Double, x: Float, y: Float): Pair<Any?, Vec> {
        // Create ray in inverted projection space.
        val origin = projection.inv * Vec(x, y, 1f)
        val direction = projection.inv.rotate(-Vec.Z)

        // Create tree map from captures.
        val sortedCaptures = TreeMap(captures)

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
     * Ends the block.
     */
    fun end() {
        captures.clear()
    }

    /**
     * Submits a capture call.
     */
    override fun <T> submit(subject: Capturable<T>, args: T, result: Any, time: Double, transform: Mat) {
        // Validate time and bounds.
        validate(subject, time, transform) {
            // Add a capture call on the correct Z index.
            captures.getOrPut(transform.center.z, ::mutableListOf).add(
                Entry(subject, args, result, transform)
            )
        }
    }
}