package eu.metatools.fio.playable

import eu.metatools.fio.end
import eu.metatools.fio.data.Mat

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