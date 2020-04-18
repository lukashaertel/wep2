package eu.metatools.sx.index

import java.util.*

/**
 * Stores values directly in a backing tree map.
 */
class IndexTree<K : Comparable<K>, V> : BaseIndex<K, V>() {

    /**
     * The items that are stored in this direct index.
     */
    private val items = TreeMap<K, V>()

    override fun put(key: K, value: V): V? {
        // Assign in item index.
        val previous = items.put(key, value)

        // If no change occurred, return just the previous value.
        if (previous == value)
            return previous

        // Disambiguate cases of delta generation.
        if (previous != null) {
            // Previous was present, check if actual change.
            if (previous != value)
                publish(key, DeltaChange(previous, value))
        } else {
            // Previous was not present, produce add
            publish(key, DeltaAdd(value))
        }

        // Return previous assignment.
        return previous
    }

    override fun remove(key: K): V? {
        // Remove value, if nothing was removed, return false.
        val previous = items.remove(key)

        // If actual change occurred, propagate delta.
        if (previous != null)
            publish(key, DeltaRemove(previous))

        // Return true as change occurred.
        return previous
    }

    override fun find(query: Query<K>): Sequence<Pair<K, V>> =
        when (query) {
            // For always, return all entries as pairs.
            is Always<K> ->
                items.entries
                    .asSequence()
                    .map { it.toPair() }

            // For never, return empty sequence.
            is Never<K> ->
                emptySequence()

            // For at, return exact entry or empty sequence if not present.
            is At<K> ->
                items[query.key]?.let { sequenceOf(query.key to it) } ?: sequenceOf()

            // For lower, return entries of tail-map as pairs.
            is After<K> ->
                items.tailMap(query.key, true)
                    .entries
                    .asSequence()
                    .map { it.toPair() }

            // For upper, return entries of head-map as pairs.
            is Before<K> ->
                items.headMap(query.key, true)
                    .entries
                    .asSequence()
                    .map { it.toPair() }

            // For range, return entries of sub-map as pairs.
            is Between<K> ->
                items.subMap(query.keyLower, true, query.keyUpper, true)
                    .entries
                    .asSequence()
                    .map { it.toPair() }
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IndexTree<*, *>

        if (items != other.items) return false

        return true
    }

    override fun hashCode(): Int {
        return items.hashCode()
    }

    override fun toString() =
        buildString {
            append("Tree(items={")
            var first = true
            for ((k, v) in items.entries.take(3)) {
                if (!first)
                    append(", ")
                append(k)
                append("=")
                append(v)
                first = false
            }
            if (items.size > 3)
                append(", ...")
            append("})")
        }
}