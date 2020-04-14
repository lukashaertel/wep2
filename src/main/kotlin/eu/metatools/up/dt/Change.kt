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

    /**
     * True if actually a change.
     */
    fun isChange() = !equals(invert())
}