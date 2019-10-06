package eu.metatools.wep2.util.collections

import eu.metatools.wep2.util.listeners.MapListener
import java.util.*

/**
 * The listener for an [ObservableMap].
 */
typealias ObservableMapListener<K, V> = MapListener<ObservableMap<K, V>, K, V>


/**
 * Simplified observable map.
 * @param listener The listener to notify..
 */
class ObservableMap<K : Comparable<K>, V>(
    val listener: ObservableMapListener<K, V>
) : SimpleMap<K, V> {

    private val backing = TreeMap<K, V>()

    override val isEmpty
        get() = backing.isEmpty()

    /**
     * The keys as a set.
     */
    override val keys get() = backing.keys.toSet()

    /**
     * The values as a list.
     */
    override val values get() = backing.values.toList()

    /**
     * Blocks calls to the change listeners.
     */
    val silent by lazy {
        object : SimpleMap<K, V> {
            override val isEmpty
                get() = this@ObservableMap.isEmpty

            override val keys
                get() = this@ObservableMap.keys

            override val values
                get() = this@ObservableMap.values

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
            listener.added(this, key, value)
            return before
        }

        // Old value differing, notify change.
        if (before != value) {
            listener.changed(this, key, before, value)
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
            listener.removed(this, key, before)
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