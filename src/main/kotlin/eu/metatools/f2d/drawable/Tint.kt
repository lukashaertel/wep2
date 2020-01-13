package eu.metatools.f2d.drawable

import com.badlogic.gdx.graphics.Color
import eu.metatools.f2d.context.Context

/**
 * Runs the [Drawable]s [Drawable.draw] method with a different color.
 */
fun <T> Drawable<T>.tint(tint: Color) = object : Drawable<T> {
    override fun draw(args: T, time: Double, context: Context) {
        // Get original values.
        val previous = context.color

        // Set new values.
        context.color = context.color.mul(tint)

        // Draw with updated context.
        this@tint.draw(args, time, context)

        // Reset.
        context.color = previous
    }
}