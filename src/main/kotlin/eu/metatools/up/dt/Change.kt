package eu.metatools.up.dt

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