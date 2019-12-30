package eu.metatools.f2d.resource

import eu.metatools.f2d.drawable.Drawable
import eu.metatools.f2d.playable.Playable

/**
 * Interface of exposed lifecycle handlers.
 */
interface Lifecycle {
    /**
     * Loads the underlying data.
     */
    fun initialize()

    /**
     * Disposes of the resource.
     */
    fun dispose()
}

/**
 * A resource with lifecycle.
 */
interface LifecycleResource<in A, out I> : Lifecycle,
    Resource<A, I>

/**
 * A drawable with lifecycle.
 */
interface LifecycleDrawable<in T> : Lifecycle,
    Drawable<T>

/**
 * A playable with lifecycle.
 */
interface LifecyclePlayable<in T> : Lifecycle,
    Playable<T>