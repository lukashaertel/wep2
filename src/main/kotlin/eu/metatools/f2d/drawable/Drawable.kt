package eu.metatools.f2d.drawable

import eu.metatools.f2d.Timed
import eu.metatools.f2d.context.Context

/**
 * A drawable instance.
 */
interface Drawable<in T> : Timed {
    /**
     * Generates calls to the sprite batch.
     */
    fun draw(args: T, time: Double, context: Context)
}


