package eu.metatools.f2d.drawable

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import eu.metatools.f2d.context.Context
import eu.metatools.f2d.end

/**
 * Chains the receiver with the next [Drawable]. If receiver does not end, [next] will never draw.
 * The argument type is generic and arguments are extracted via [firstArg] and [secondArg].
 */
fun <T, A, B> Drawable<A>.then(next: Drawable<B>, firstArg: (T) -> A, secondArg: (T) -> B) =
    object : Drawable<T> {
        override fun draw(args: T, time: Double, context: Context) {
            if (time < this@then.end)
                this@then.draw(firstArg(args), time, context)
            else
                next.draw(secondArg(args), time - this@then.duration, context)
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