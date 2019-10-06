package eu.metatools.wep2.util.collections

import eu.metatools.wep2.util.listeners.SetListener
import java.util.*

/**
 * The listener for an [ObservableSet].
 */
typealias ObservableSetListener<E> = SetListener<ObservableSet<E>, E>

/**
 * Simplified observable set.
 * @param listener The listener to notify.
 */
class ObservableSet<E : Comparable<E>>(
    val listener: ObservableSetListener<E>
) : SimpleSet<E> {
    private val backing = TreeSet<E>()

    override val isEmpty
        get() = backing.isEmpty()

    /**
     * Blocks calls to the change listeners.
     */
    val silent by lazy {
        object : SimpleSet<E> {
            override val isEmpty: Boolean
                get() = this@ObservableSet.isEmpty

            override fun add(element: E) =
                backing.add(element)

            override fun remove(element: E) =
                backing.remove(element)

            override fun contains(element: E) =
                backing.contains(element)

            override fun iterator() =
                backing.iterator()
        }
    }

    override fun add(element: E): Boolean {
        if (!backing.add(element))
            return false

        listener.added(this, element)

        return true
    }

    override fun remove(element: E): Boolean {
        if (!backing.remove(element))
            return false

        listener.removed(this, element)

        return true
    }

    override operator fun contains(element: E) =
        backing.contains(element)

    override fun iterator() =
        backing.iterator()

    override fun toString() =
        backing.toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other is ObservableSet<*>)
            return backing == other.backing

        if (other is SimpleSet<*>)
            return toSet() == other.toSet()

        return false
    }

    override fun hashCode(): Int {
        return backing.hashCode()
    }
}

