package eu.metatools.f2d.context

import com.badlogic.gdx.graphics.g2d.SpriteBatch

/**
 * A drawable instance.
 */
interface Drawable<in T> : Timed {
    /**
     * Generates calls to the sprite batch.
     */
    fun generate(args: T, time: Double, receiver: ((SpriteBatch) -> Unit) -> Unit)
}

/**
 * Returns a drawable instance that is fixed to end after the given time.
 */
infix fun <T> Drawable<T>.limit(duration: Double) = object : Drawable<T> {
    override fun generate(args: T, time: Double, receiver: ((SpriteBatch) -> Unit) -> Unit) =
        this@limit.generate(args, time, receiver)

    override val start: Double
        get() = this@limit.start

    override val duration: Double
        get() = minOf(this@limit.duration, duration)
}

/**
 * Returns a drawable instance that is offset by the given time.
 */
infix fun <T> Drawable<T>.offset(offset: Double) = object : Drawable<T> {
    override fun generate(args: T, time: Double, receiver: ((SpriteBatch) -> Unit) -> Unit) =
        this@offset.generate(args, time - offset, receiver)

    override val start: Double
        get() = this@offset.start + offset

    override val duration: Double
        get() = this@offset.duration
}

/**
 * Chains the receiver with the next [Drawable]. If receiver does not end, [next] will never draw.
 */
infix fun <T> Drawable<T>.then(next: Drawable<T>) = object : Drawable<T> {
    override fun generate(args: T, time: Double, receiver: ((SpriteBatch) -> Unit) -> Unit) {
        if (time < this@then.end)
            this@then.generate(args, time, receiver)
        else
            next.generate(args, time - this@then.duration, receiver)
    }

    override val duration: Double
        get() = this@then.duration + next.duration

    override val start: Double
        get() = this@then.start
}