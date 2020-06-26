package eu.metatools.fio.drawable

import eu.metatools.fio.Timed
import eu.metatools.fio.context.Context

/**
 * A drawable instance.
 */
interface Drawable<in T> : Timed {
    /**
     * Generates calls to the sprite batch.
     */
    fun draw(args: T, time: Double, context: Context)
}


