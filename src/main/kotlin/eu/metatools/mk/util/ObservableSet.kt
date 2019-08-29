package eu.metatools.mk.util

/**
 * A simpler interface for mutable sets.
 */
interface SimpleSet<E> : Iterable<E> {
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

/**
 * Simplified observable set.
 */
abstract class ObservableSet<E> : SimpleSet<E> {
    private val backing = mutableSetOf<E>()

    /**
     * Blocks calls to the change listeners.
     */
    val silent by lazy {
        object : SimpleSet<E> {
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

        added(element)
        return true
    }

    override fun remove(element: E): Boolean {
        if (!backing.remove(element))
            return false

        removed(element)
        return true
    }

    override operator fun contains(element: E) =
        backing.contains(element)

    override fun iterator() =
        backing.iterator()

    /**
     * Called when a value was actually added.
     */
    protected abstract fun added(element: E)

    /**
     * Called when a value was actually removed.
     */
    protected abstract fun removed(element: E)

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

/**
 * Creates a simplified observable set with the given delegates.
 */
inline fun <E> observableSet(
    crossinline added: ObservableSet<E>.(E) -> Unit,
    crossinline removed: ObservableSet<E>.(E) -> Unit
) = object : ObservableSet<E>() {
    override fun added(element: E) =
        added(this, element)

    override fun removed(element: E) =
        removed(this, element)
}