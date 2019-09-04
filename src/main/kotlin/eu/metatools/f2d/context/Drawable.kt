package eu.metatools.f2d.context

import com.badlogic.gdx.graphics.g2d.SpriteBatch

/**
 * A drawable instance.
 */
interface Drawable<in T> : Lifetime<T> {
    /**
     * Generates calls to the sprite batch.
     */
    fun generate(args: T, time: Double, receiver: ((SpriteBatch) -> Unit) -> Unit)
}

/**
 * Returns a drawable instance that is fixed to end after the given time.
 */
fun <T> Drawable<T>.limit(endTime: Double) = object : Drawable<T> {
    override fun generate(args: T, time: Double, receiver: ((SpriteBatch) -> Unit) -> Unit) =
        this@limit.generate(args, time, receiver)

    override fun hasStarted(args: T, time: Double) =
        this@limit.hasStarted(args, time)

    override fun hasEnded(args: T, time: Double) =
        time > endTime
}

/**
 * Returns a drawable instance that is offset by the given time.
 */
fun <T> Drawable<T>.offset(offset: Double) = object : Drawable<T> {
    override fun generate(args: T, time: Double, receiver: ((SpriteBatch) -> Unit) -> Unit) =
        this@offset.generate(args, time - offset, receiver)

    override fun hasStarted(args: T, time: Double) =
        this@offset.hasStarted(args, time - offset)

    override fun hasEnded(args: T, time: Double) =
        this@offset.hasEnded(args, time - offset)
}