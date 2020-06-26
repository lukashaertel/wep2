package eu.metatools.fio.drawable

import eu.metatools.fio.context.Context

/**
 * Returns a drawable instance that is offset by the given time.
 */
infix fun <T> Drawable<T>.offset(offset: Double) = object :
    Drawable<T> {
    override fun draw(args: T, time: Double, context: Context) =
        this@offset.draw(args, time - offset, context)

    override val start: Double
        get() = this@offset.start + offset

    override val duration: Double
        get() = this@offset.duration
}