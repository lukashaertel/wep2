package eu.metatools.f2d.immediate

import com.badlogic.gdx.graphics.Color
import eu.metatools.f2d.context.Context
import eu.metatools.f2d.data.Blend
import eu.metatools.f2d.data.Mat
import eu.metatools.f2d.drawable.Drawable
import eu.metatools.f2d.end
import java.util.*

/**
 * Standard implementation of [Immediate].
 * @property trimExcess How far outside of the view a center must lie for the subject to be ignored.
 */
class StandardDraw(trimExcess: Float) : ProjectionTrimmed(trimExcess), Draw {
    /**
     * Draw in the current [begin]/[end] block.
     */
    private data class Entry<T>(val subject: Drawable<T>, val args: T, val transform: Mat) {
        fun draw(time: Double, context: Context) {
            // Override transform.
            context.transform = context.transform.set(transform.values)

            // Draw subject itself.
            subject.draw(args, time, context)
        }
    }

    /**
     * All draws in the current block.
     */
    private val draws = hashMapOf<Float, MutableList<Entry<*>>>()

    /**
     * Starts the block with the given matrices.
     */
    fun begin(projection: Mat) {
        this.projection = projection
    }

    /**
     * Renders all draw calls in this block.
     */
    fun render(time: Double, context: Context) {
        context.color = Color.WHITE
        context.blend = Blend.DEFAULT
        context.transform = context.transform.idt()
        context.projection = context.projection.set(projection.values)

        // Create tree map from draws.
        val sortedDraws = TreeMap(draws).descendingMap()

        // Iterate all Z-layers and all draw calls.
        for ((_, draws) in sortedDraws)
            for (draw in draws)
                draw.draw(time, context)

        // Finalize context.
        context.none()
    }

    /**
     * Ends the block.
     */
    fun end() {
        draws.clear()
    }

    /**
     * Submits a draw call.
     */
    override fun <T> submit(subject: Drawable<T>, args: T, time: Double, transform: Mat) {
        // Validate time and bounds.
        validate(subject, time, transform) {
            // Add a draw call on the correct Z index.
            draws.getOrPut(transform.center.z, ::mutableListOf).add(
                Entry(subject, args, transform)
            )
        }
    }
}