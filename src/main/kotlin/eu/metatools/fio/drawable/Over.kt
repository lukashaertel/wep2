package eu.metatools.fio.drawable

import eu.metatools.fio.context.Context

infix fun <T> Drawable<T>.over(other: Drawable<T>) = object :
    Drawable<T> {
    override fun draw(args: T, time: Double, context: Context) {
        other.draw(args, time, context)
        this@over.draw(args, time, context)
    }
}
