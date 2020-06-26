package eu.metatools.fio.drawable

import com.badlogic.gdx.graphics.Color
import eu.metatools.fio.context.Context

/**
 * Runs the [Drawable]s [Drawable.draw] method with a different color.
 */
fun <T> Drawable<T>.color(color: Color) = object : Drawable<T> {
    override fun draw(args: T, time: Double, context: Context) {
        // Get original values.
        val previous = context.color

        // Set new values.
        context.color = color

        // Draw with updated context.
        this@color.draw(args, time, context)

        // Reset.
        context.color = previous
    }
}