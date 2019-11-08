package eu.metatools.up.aspects

import eu.metatools.up.dt.Lx

/**
 * A structured change.
 */
interface Change<T : Change<T>> {
    /**
     * Merges the receiver with [other].
     */
    fun merge(other: T): T

    /**
     * Inverts the receiver.
     */
    fun invert(): T
}

/**
 * Force merges the change with the other. Ignores the out-projected type.
 */
fun Change<*>.mergeForce(other: Change<*>) =
    @Suppress("member_projected_out")
    merge(other)

/**
 * Listens to views and changes.
 */
interface Listen : Aspect {
    /**
     * Notifies that the element with the [id] was viewed.
     */
    fun viewed(id: Lx, value: Any?) = Unit

    /**
     * Notifies that the element with the [id] was changed as defined by [change].
     */
    fun changed(id: Lx, change: Change<*>) = Unit
}
