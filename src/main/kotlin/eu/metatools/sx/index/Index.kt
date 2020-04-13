package eu.metatools.sx.index

/**
 * A delta providing index that can be added to or removed from and that
 * provides resolution of queries.
 */
abstract class Index<K : Comparable<K>, V> {
    /**
     * Registers a listener that will be notified when the passed query is satisfied.
     * @param query The query that must be met.
    // * @param block The receiver, will take key, old value if present and new value if present.
     * @return Returns a closable that will unregister the listener.
     */
    abstract fun register(query: Query<K>, block: (K, Delta<V>) -> Unit): AutoCloseable

    /**
     * Adds a value to the index.
     * @param key The key under which the value is added.
     * @param value The value to add.
     * @return Returns the previous value.
     */
    abstract fun put(key: K, value: V): V?

    /**
     * Removes a value from the index.
     * @param key The key of the value to remove.
     * @return Returns the assigned value.
     */
    abstract fun remove(key: K): V?

    /**
     * Finds all entries satisfying a query.
     */
    abstract fun find(query: Query<K>): Sequence<Pair<K, V>>

    /**
     * Moves an entry from the given key to another. Returns the old entry at the target.
     * If no entry was at the original location, nothing will be moved.
     */
    open fun move(keyFrom: K, keyTo: K): V? =
        remove(keyFrom)?.let {
            put(keyTo, it)
        }
}

