package eu.metatools.f2d.context

import eu.metatools.f2d.math.Mat

/**
 * A playable instance.
 */
interface Playable<in T> : Timed {
    /**
     * Starts or updates the instance with the given [handle].
     */
    fun play(args: T, handle: Any, time: Double, transform: Mat)

    /**
     * Cancels the instance with the given internal handle.
     */
    fun cancel(handle: Any)
}

/**
 * Returns a playable instance that is fixed to end after the given time.
 */
infix fun <T> Playable<T>.limit(duration: Double) = object : Playable<T> {
    override fun play(args: T, handle: Any, time: Double, transform: Mat) =
        this@limit.play(args, handle, time, transform)

    override fun cancel(handle: Any) =
        this@limit.cancel(handle)

    override val start: Double
        get() = this@limit.start

    override val duration: Double
        get() = minOf(this@limit.duration, duration)
}

/**
 * Returns a playable instance that is offset by the given time.
 */
infix fun <T> Playable<T>.offset(offset: Double) = object : Playable<T> {
    override fun play(args: T, handle: Any, time: Double, transform: Mat) =
        this@offset.play(args, handle, time - offset, transform)

    override fun cancel(handle: Any) =
        this@offset.cancel(handle)

    override val start: Double
        get() = this@offset.start + offset

    override val duration: Double
        get() = this@offset.duration
}

private val thenLeftHandle = Any()
private val thenRightHandle = Any()

/**
 * Chains the receiver with the next [Playable]. If receiver does not end, [next] will never play.
 * The argument type is generic and arguments are extracted via [firstArg] and [secondArg].
 */
fun <T, A, B> Playable<A>.then(next: Playable<B>, firstArg: (T) -> A, secondArg: (T) -> B) =
    object : Playable<T> {
        override fun play(args: T, handle: Any, time: Double, transform: Mat) {
            if (time < this@then.end)
                this@then.play(firstArg(args), handle to thenLeftHandle, time, transform)
            else
                next.play(secondArg(args), handle to thenRightHandle, time - this@then.duration, transform)
        }

        override fun cancel(handle: Any) {
            this@then.cancel(handle to thenLeftHandle)
            next.cancel(handle to thenRightHandle)
        }

        override val duration: Double
            get() = this@then.duration + next.duration

        override val start: Double
            get() = this@then.start
    }

/**
 * Concatenates two [Playable]s with equal argument types, using the same argument.
 */
infix fun <T> Playable<T>.then(next: Playable<T>) = then<T, T, T>(next, { it }, { it })

/**
 * Concatenates two [Playable]s with different argument types, resulting in a pair.
 */
infix fun <T, U> Playable<T>.thenPair(next: Playable<U>) =
    then<Pair<T, U>, T, U>(next, { it.first }, { it.second })


/**
 * Concatenates two [Playable]s with different argument types, resulting in a pair.
 */
@JvmName("thenPairNullable")
infix fun <T : Any, U : Any> Playable<T?>.thenPair(next: Playable<U?>) =
    then<Pair<T, U>?, T?, U?>(next, { it?.first }, { it?.second })