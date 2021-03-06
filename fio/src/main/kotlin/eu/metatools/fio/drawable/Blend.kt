package eu.metatools.fio.drawable

import eu.metatools.fio.context.Context
import eu.metatools.fio.data.Blend

/**
 * Runs the [Drawable]s [Drawable.draw] method with a different blend-function.
 */
fun <T> Drawable<T>.blend(src: Int, dst: Int, srcAlpha: Int = src, dstAlpha: Int = dst) = object : Drawable<T> {
    override fun draw(args: T, time: Double, context: Context) {
        // Get original values.
        val previous = context.blend

        // Set new values.
        context.blend = Blend(src, dst, srcAlpha, dstAlpha)

        // Draw with updated context.
        this@blend.draw(args, time, context)

        // Reset.
        context.blend = previous
    }
}