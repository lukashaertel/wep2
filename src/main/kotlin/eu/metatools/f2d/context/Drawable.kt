package eu.metatools.f2d.context

import com.badlogic.gdx.graphics.g2d.SpriteBatch

/**
 * A drawable instance.
 */
interface Drawable<in T> : Timed {
    /**
     * Generates calls to the sprite batch.
     */
    fun draw(args: T, time: Double, receiver: ((SpriteBatch) -> Unit) -> Unit)
}

/**
 * Returns a drawable instance that is fixed to end after the given time.
 */
infix fun <T> Drawable<T>.limit(duration: Double) = object : Drawable<T> {
    override fun draw(args: T, time: Double, receiver: ((SpriteBatch) -> Unit) -> Unit) =
        this@limit.draw(args, time, receiver)

    override val start: Double
        get() = this@limit.start

    override val duration: Double
        get() = minOf(this@limit.duration, duration)
}

/**
 * Returns a drawable instance that is offset by the given time.
 */
infix fun <T> Drawable<T>.offset(offset: Double) = object : Drawable<T> {
    override fun draw(args: T, time: Double, receiver: ((SpriteBatch) -> Unit) -> Unit) =
        this@offset.draw(args, time - offset, receiver)

    override val start: Double
        get() = this@offset.start + offset

    override val duration: Double
        get() = this@offset.duration
}

/**
 * Chains the receiver with the next [Drawable]. If receiver does not end, [next] will never draw.
 * The argument type is generic and arguments are extracted via [firstArg] and [secondArg].
 */
fun <T, A, B> Drawable<A>.then(
    next: Drawable<B>,
    firstArg: (T) -> A,
    secondArg: (T) -> B
) = object : Drawable<T> {
    override fun draw(args: T, time: Double, receiver: ((SpriteBatch) -> Unit) -> Unit) {
        if (time < this@then.end)
            this@then.draw(firstArg(args), time, receiver)
        else
            next.draw(secondArg(args), time - this@then.duration, receiver)
    }

    override val duration: Double
        get() = this@then.duration + next.duration

    override val start: Double
        get() = this@then.start
}


/**
 * Concatenates two [Drawable]s with equal argument types, using the same argument.
 */
infix fun <T> Drawable<T>.then(next: Drawable<T>) = then<T, T, T>(next, { it }, { it })


/**
 * Concatenates two [Drawable]s with different argument types, resulting in a pair.
 */
infix fun <T, U> Drawable<T>.thenPair(next: Drawable<U>) = then<Pair<T, U>, T, U>(next, { it.first }, { it.second })


/**
 * Concatenates two [Drawable]s with different argument types, resulting in a pair.
 */
@JvmName("thenPairNullable")
infix fun <T : Any, U : Any> Drawable<T?>.thenPair(next: Drawable<U?>) =
    then<Pair<T, U>?, T?, U?>(next, { it?.first }, { it?.second })