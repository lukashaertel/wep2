package eu.metatools.wep2.util

/**
 * A simpler interface for mutable maps.
 */
interface SimpleMap<K, V> : Iterable<Map.Entry<K, V>> {
    /**
     * Sets the key to the value, returns the previous association or null.
     */
    operator fun set(key: K, value: V): V?

    /**
     * Sets the value for the key, returns the previous association or null.
     */
    fun remove(key: K): V?

    /**
     * Returns the value associated to the key or null.
     */
    operator fun get(key: K): V?

    /**
     * True if the key is associated in the map.
     */
    operator fun contains(key: K): Boolean
}

/**
 * Simplified observable map.
 */
abstract class ObservableMap<K, V> : SimpleMap<K, V> {
    private val backing = mutableMapOf<K, V>()

    /**
     * Blocks calls to the change listeners.
     */
    val silent by lazy {
        object : SimpleMap<K, V> {
            override fun set(key: K, value: V) =
                backing.put(key, value)

            override fun remove(key: K) =
                backing.remove(key)

            override fun get(key: K) =
                backing[key]

            override fun contains(key: K) =
                backing.contains(key)

            override fun iterator() =
                backing.iterator()
        }
    }

    override operator fun set(key: K, value: V): V? {
        // Put in backing.
        val before = backing.put(key, value)

        // Old value not present, notify add.
        if (before == null) {
            added(key, value)
            return before
        }

        // Old value differing, notify change.
        if (before != value) {
            changed(key, before, value)
            return before
        }

        // No notification necessary.
        return before
    }

    override fun remove(key: K): V? {
        // Remove in backing.
        val before = backing.remove(key)

        // Old value was present, notify removal.
        if (before != null) {
            removed(key, before)
            return before
        }

        // No notification necessary.
        return before
    }

    override operator fun get(key: K) =
        backing[key]

    override operator fun contains(key: K) =
        backing.contains(key)

    override fun iterator() =
        backing.iterator()

    /**
     * Called when an entry is actually added.
     */
    protected abstract fun added(key: K, value: V)

    /**
     * Called when an entry is actually changed.
     */
    protected abstract fun changed(key: K, oldValue: V, newValue: V)

    /**
     * Called when an entry is actually removed.
     */
    protected abstract fun removed(key: K, value: V)

    override fun toString() =
        backing.toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other is ObservableMap<*, *>)
            return backing == other.backing

        if (other is SimpleMap<*, *>)
            return toSet() == other.toSet()

        return false
    }

    override fun hashCode(): Int {
        return backing.hashCode()
    }
}

/**
 * Creates a simplified observable map with the given delegates.
 */
inline fun <K, V> observableMap(
    crossinline added: ObservableMap<K, V>.(K, V) -> Unit,
    crossinline changed: ObservableMap<K, V>.(K, V, V) -> Unit,
    crossinline removed: ObservableMap<K, V>.(K, V) -> Unit
) = object : ObservableMap<K, V>() {
    override fun added(key: K, value: V) =
        added(this, key, value)

    override fun changed(key: K, oldValue: V, newValue: V) =
        changed(this, key, oldValue, newValue)

    override fun removed(key: K, value: V) =
        removed(this, key, value)
}