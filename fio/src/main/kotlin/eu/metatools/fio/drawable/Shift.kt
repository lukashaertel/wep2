package eu.metatools.fio.drawable

import eu.metatools.fio.context.Context
import eu.metatools.fio.data.Mat
import eu.metatools.fio.data.Pt
import eu.metatools.fio.data.Vec

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

/**
 * Shifts the transformation matrix by the given amount.
 */
fun <T> Drawable<T>.shift(x: Float, y: Float, z: Float) = object :
    Drawable<T> {
    override fun draw(args: T, time: Double, context: Context) {
        // Get x, y and z in coordinate system.
        val (xPrime, yPrime, zPrime) = Mat(context.transform.values).rotate(Vec(x, y, z))

        // Translate, draw, translate inverse.
        context.transform = context.transform.trn(xPrime, yPrime, zPrime)
        this@shift.draw(args, time, context)
        context.transform = context.transform.trn(-xPrime, -yPrime, -zPrime)
    }
}