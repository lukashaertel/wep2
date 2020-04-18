package eu.metatools.sx.index

import eu.metatools.sx.util.Then
import eu.metatools.sx.util.then

/**
 * For an index that's values are lists of elements, returns a new index that lifts the indices
 * of the values into the key structure, returning a flattened front of values.
 * @property it The index to flatten.
 * @property zero The default value generator or null, if out of bounds puts are not supported.
 */
data class Flatten<K : Comparable<K>, V>(
    val it: Index<K, List<V>>,
    val zero: (() -> V)?
) : Index<Then<K, Int>, V>() {
    companion object {
        /**
         * The minimum composed key of a [Flatten] index.
         */
        fun <K : Comparable<K>> inf(key: K) =
            key then 0

        /**
         * The maximum composed key of a [Flatten] index.
         */
        fun <K : Comparable<K>> sup(key: K) =
            key then Int.MAX_VALUE
    }

    /**
     * The default instance, if no default is given, this will default to an instance
     * that throws an unsupported operation exception.
     */
    private val zeroInstance = zero ?: {
        throw UnsupportedOperationException("Flatten does not support out of bounds puts on index $it")
    }

    /**
     * Constructs a flatten index without out of bounds puts.
     * @param it The index to flatten.
     */
    constructor(it: Index<K, List<V>>) : this(it, null)

    /**
     * Registers a query where the positional coordinate of the actual key is disregarded. See [inf] and [sup].
     * @param query The query that must be met on the key's first coordinate.
     * @param block The delta receiver.
     * @return Returns a closable that will unregister the listener.
     */
    fun registerFlat(query: Query<K>, block: (Then<K, Int>, Delta<V>) -> Unit): AutoCloseable {
        // Transform coordinateless queries.
        val transformed: Query<Then<K, Int>> = when (query) {
            is Always -> Always()
            is Never -> Never()
            is At -> Between(inf(query.key), sup(query.key))
            is After -> After(inf(query.key))
            is Before -> Before(sup(query.key))
            is Between -> Between(inf(query.keyLower), sup(query.keyUpper))
        }

        // Register the transformed query.
        return register(transformed, block)
    }

    override fun register(
        query: Query<Then<K, Int>>,
        block: (Then<K, Int>, Delta<V>) -> Unit
    ): AutoCloseable {
        // Split query.
        val (queryLeft, queryRight) = split(query)
        return it.register(queryLeft) { k, d ->
            when (d) {
                // When entire list is added, generate an add for all matching results.
                is DeltaAdd -> d.added.mapIndexed { i, v ->
                    i to v
                }.filter { (i, _) ->
                    queryRight(i)
                }.forEach { (i, v) ->
                    block(k then i, DeltaAdd(v))
                }

                // When entire list is removed, generate a remove for all matching results.
                is DeltaRemove -> d.removed.mapIndexed { i, v ->
                    i to v
                }.filter { (i, _) ->
                    queryRight(i)
                }.forEach { (i, v) ->
                    block(k then i, DeltaRemove(v))
                }

                is DeltaChange -> {
                    // Create map of old associations.
                    val old = d.from.mapIndexed { i, v ->
                        i to v
                    }.filter { (i, _) ->
                        queryRight(i)
                    }.associate {
                        it
                    }

                    // Create map of new associations.
                    val new = d.to.mapIndexed { i, v ->
                        i to v
                    }.filter { (i, _) ->
                        queryRight(i)
                    }.associate {
                        it
                    }

                    // Produce all removes.
                    for (i in old.keys subtract new.keys)
                        block(k then i, DeltaRemove(old.getValue(i)))

                    // Produce value changes.
                    for (i in old.keys intersect new.keys) {
                        val out = DeltaChange(old.getValue(i), new.getValue(i))
                        if (out.isChange())
                            block(k then i, out)
                    }


                    // Produce all adds.
                    for (i in new.keys subtract old.keys)
                        block(k then i, DeltaAdd(new.getValue(i)))
                }
            }
        }
    }

    override fun put(key: Then<K, Int>, value: V): V? {
        // Get existing list.
        val existing = it.find(At(key.first)).singleOrNull()

        // Existing list is null.
        if (existing == null) {
            // Make new list to accommodate for desired key.
            val new = List(key.second + 1) {
                when (it) {
                    // At key, return value.
                    key.second -> value

                    // Otherwise default.
                    else -> zeroInstance()
                }
            }

            // Write list, return null as no value could be assigned.
            it.put(key.first, new)
            return null

        }

        // Make new list with existing values where possible, new key where assigned and defaults for all other places.
        val new = List(maxOf(existing.second.size, key.second + 1)) {
            when (it) {
                // At key, return value.
                key.second -> value

                // Available in existing list, get value.
                in existing.second.indices -> existing.second[it]

                // Otherwise default.
                else -> zeroInstance()
            }
        }

        // Write list.
        it.put(key.first, new)

        // If existing had key, return the value, otherwise null.
        return if (key.second in existing.second.indices)
            existing.second[key.second]
        else
            null
    }

    override fun remove(key: Then<K, Int>): V? {
        // Find the source list, if not present, return null.
        val existing = it.find(At(key.first)).singleOrNull()
            ?: return null

        // Key index is not in the list, return null.
        if (existing.second.size <= key.second)
            return null

        // Make new list skipping the position.
        val new = List(existing.second.size - 1) {
            when {
                // Index before removed key, get from existing.
                it < key.second -> existing.second[it]

                // Otherwise, get from existing while skipping one.
                else -> existing.second[it + 1]
            }
        }

        // Add new value to source.
        it.put(key.first, new)

        // Return what was in the old list.
        return existing.second[key.second]
    }

    override fun find(query: Query<Then<K, Int>>): Sequence<Pair<Then<K, Int>, V>> {
        // Split query.
        val (queryLeft, queryRight) = split(query)

        // Find left part of the query in source.
        return it.find(queryLeft).flatMap { (k, vs) ->
            // Flat map the results of those, include index, filter with the second part of the query.
            vs.asSequence().mapIndexed { i, v -> (k then i) to v }
                .filter { queryRight(it.first.second) }
        }
    }
}

/**
 * Creates a [Flatten] index with the given args.
 */
fun <K : Comparable<K>, V> Index<K, List<V>>.flatten() =
    Flatten(this)

/**
 * Creates a [Flatten] index with the given args.
 */
fun <K : Comparable<K>, V> Index<K, List<V>>.flatten(zero: () -> V) =
    Flatten(this, zero)


/**
 * For a flatten index, applies sorted set semantics and inserts the value into the list.
 * @param key The first coordinate of the key.
 * @param value The value of the key.
 * @return Returns the previous assignment.
 */
fun <K : Comparable<K>, V : Comparable<V>> Flatten<K, V>.add(key: K, value: V): List<V>? {
    // Find existing list.
    val existing = it.find(At(key)).singleOrNull()

    // Existing is null or empty, add singleton.
    if (existing == null || existing.second.isEmpty())
        return it.put(key, listOf(value))

    // Some elements are present, find insertion point.
    val at = existing.second.binarySearch(value)

    // Element is already in list, return existing.
    if (at >= 0)
        return existing.second

    // Not existing, get index of the inserted value.
    val flip = -at - 1

    // Intersperse lists.
    val new = List(existing.second.size.inc()) {
        when {
            // Before the insertion point, get from existing.
            it < flip -> existing.second[it]

            // At insertion point, get value.
            it == flip -> value

            // After insertion point, get from existing again.
            else -> existing.second[it - 1]
        }
    }

    // Add new list for result.
    return it.put(key, new)
}

/**
 * For a flatten index, applies sorted set semantics and removes the value from the list.
 * @param key The first coordinate of the key.
 * @param value The value of the key.
 * @return Returns the previous assignment.
 */
fun <K : Comparable<K>, V : Comparable<V>> Flatten<K, V>.remove(key: K, value: V): List<V>? {
    // Get existing list, if not present, nothing to remove.
    val existing = it.find(At(key)).singleOrNull()
        ?: return null

    // Get index of value.
    val at = existing.second.binarySearch(value)

    // Value not in list, return existing, unchanged.
    if (at < 0)
        return existing.second

    // If value exists and size of list is exactly one, remove from source.
    if (existing.second.size == 1)
        return it.remove(key)

    // Create new list, skipping the removed element.
    val new = List(existing.second.size - 1) {
        when {
            // If before removal point, get from existing.
            it < at -> existing.second[it]

            // If on or after removal point, get from existing while skipping one.
            else -> existing.second[it + 1]
        }
    }

    // Add new list for result.
    return it.put(key, new)
}

/**
 * Gets the sum of the strings.
 */
fun sum(strings: Iterable<String>) =
    strings.fold(StringBuilder()) { b, s -> b.append(s) }.toString()

fun main() {

    val ti = IndexTree<Int, String>()
    ti.register(Always()) { k, d ->
        println("Base $k: $d")
    }

    val sm = ti.selectMany({ " " }, ::sum) { it.map(Char::toString) }
    sm.register(Always()) { k, d ->
        println("Select Many $k: $d")
    }

    ti.put(1, "Hello xorld")
    sm.put(1 then 6, "W")
    sm.put(1 then 11, ".")
    sm.put(1 then 13, "O")
    sm.put(1 then 14, "w")
    sm.put(1 then 15, "O")

}