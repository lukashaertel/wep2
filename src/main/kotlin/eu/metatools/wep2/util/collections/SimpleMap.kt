package eu.metatools.wep2.util.collections

/**
 * A simpler interface for mutable maps.
 */
interface SimpleMap<K : Comparable<K>, V> : Iterable<Map.Entry<K, V>> {
    /**
     * True if the map is not empty.
     */
    val isEmpty: Boolean get() = iterator().hasNext()

    /**
     * Gets all keys if supported.
     */
    val keys: Set<K> get() = throw UnsupportedOperationException("This map does not expose key set")

    /**
     * Gets all values if supported.
     */
    val values: List<V> get() = throw UnsupportedOperationException("This map does not expose value list")

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