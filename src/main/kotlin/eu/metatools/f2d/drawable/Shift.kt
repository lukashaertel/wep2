package eu.metatools.f2d.drawable

import eu.metatools.f2d.context.Context
import eu.metatools.f2d.data.Mat
import eu.metatools.f2d.data.Pt

/**
 * Shifts the transformation matrix by the given amount.
 */
fun <T> Drawable<T>.shift(x: Float, y: Float) = object :
    Drawable<T> {
    override fun draw(args: T, time: Double, context: Context) {
        // Get x and y in coordinate system.
        val (xPrime, yPrime) = Mat(context.transform.values).rotate(Pt(x, y))

        // Translate, draw, translate inverse.
        context.transform = context.transform.trn(xPrime, yPrime, 0f)
        this@shift.draw(args, time, context)
        context.transform = context.transform.trn(-xPrime, -yPrime, 0f)
    }
}