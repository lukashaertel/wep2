package eu.metatools.f2d.context

import eu.metatools.f2d.math.Vec

interface Capturable<in T> : Timed {
    fun capture(args: T, time: Double, origin: Vec, direction: Vec): Vec?
}


/**
 * Returns a capturable instance that is fixed to end after the given time.
 */
infix fun <T> Capturable<T>.limit(duration: Double) = object : Capturable<T> {
    override fun capture(args: T, time: Double, origin: Vec, direction: Vec) =
        this@limit.capture(args, time, origin, direction)

    override val start: Double
        get() = this@limit.start

    override val duration: Double
        get() = minOf(this@limit.duration, duration)
}

/**
 * Returns a capturable instance that is offset by the given time.
 */
infix fun <T> Capturable<T>.offset(offset: Double) = object : Capturable<T> {
    override fun capture(args: T, time: Double, origin: Vec, direction: Vec) =
        this@offset.capture(args, time - offset, origin, direction)

    override val start: Double
        get() = this@offset.start + offset

    override val duration: Double
        get() = this@offset.duration
}

/**
 * Chains the receiver with the next [Capturable]. If receiver does not end, [next] will never capture.
 * The argument type is generic and arguments are extracted via [firstArg] and [secondArg].
 */
fun <T, A, B> Capturable<A>.then(next: Capturable<B>, firstArg: (T) -> A, secondArg: (T) -> B) =
    object : Capturable<T> {
        override fun capture(args: T, time: Double, origin: Vec, direction: Vec) =
            if (time < this@then.end)
                this@then.capture(firstArg(args), time, origin, direction)
            else
                next.capture(secondArg(args), time - this@then.duration, origin, direction)

        override val duration: Double
            get() = this@then.duration + next.duration

        override val start: Double
            get() = this@then.start
    }


/**
 * Concatenates two [Capturable]s with equal argument types, using the same argument.
 */
infix fun <T> Capturable<T>.then(next: Capturable<T>) = then<T, T, T>(next, { it }, { it })


/**
 * Concatenates two [Capturable]s with different argument types, resulting in a pair.
 */
infix fun <T, U> Capturable<T>.thenPair(next: Capturable<U>) = then<Pair<T, U>, T, U>(next, { it.first }, { it.second })


/**
 * Concatenates two [Capturable]s with different argument types, resulting in a pair.
 */
@JvmName("thenPairNullable")
infix fun <T : Any, U : Any> Capturable<T?>.thenPair(next: Capturable<U?>) =
    then<Pair<T, U>?, T?, U?>(next, { it?.first }, { it?.second })