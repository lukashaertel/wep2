package eu.metatools.wep2.util.collections

/**
 * A simpler interface for mutable sets.
 */
interface SimpleSet<E : Comparable<E>> : Iterable<E> {
    /**
     * True if the set is not empty.
     */
    val isEmpty: Boolean get() = iterator().hasNext()

    /**
     * Adds an element. True if changed.
     */
    fun add(element: E): Boolean

    /**
     * Removes an element. True if changed.
     */
    fun remove(element: E): Boolean

    /**
     * True if the element is in the set.
     */
    operator fun contains(element: E): Boolean
}