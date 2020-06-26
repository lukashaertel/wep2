package eu.metatools.fio.drawable

import eu.metatools.fio.context.Context

/**
 * Returns a drawable instance that is fixed to end after the given time.
 */
infix fun <T> Drawable<T>.limit(duration: Double) = object :
    Drawable<T> {
    override fun draw(args: T, time: Double, context:Context) =
        this@limit.draw(args, time, context)

    override val start: Double
        get() = this@limit.start

    override val duration: Double
        get() = minOf(this@limit.duration, duration)
}