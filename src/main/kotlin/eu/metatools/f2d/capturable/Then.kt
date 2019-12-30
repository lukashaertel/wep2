package eu.metatools.f2d.capturable

import eu.metatools.f2d.end
import eu.metatools.f2d.data.Vec

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