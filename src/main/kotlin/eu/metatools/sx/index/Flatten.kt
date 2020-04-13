package eu.metatools.sx.index

import eu.metatools.sx.util.ComparablePair
import eu.metatools.sx.util.toComparable

/**
 * For an index that's values are lists of elements, returns a new index that lifts the indices
 * of the values into the key structure, returning a flattened front of values.
 */
data class Flatten<K : Comparable<K>, V>(
    val it: Index<K, List<V>>
) : Index<ComparablePair<K, Int>, V>() {
    override fun register(
        query: Query<ComparablePair<K, Int>>,
        block: (ComparablePair<K, Int>, Delta<V>) -> Unit
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
                    block(k toComparable i, DeltaAdd(v))
                }

                // When entire list is removed, generate a remove for all matching results.
                is DeltaRemove -> d.removed.mapIndexed { i, v ->
                    i to v
                }.filter { (i, _) ->
                    queryRight(i)
                }.forEach { (i, v) ->
                    block(k toComparable i, DeltaRemove(v))
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
                        block(k toComparable i, DeltaRemove(old.getValue(i)))

                    // Produce value changes.
                    for (i in old.keys intersect new.keys) {
                        val out = DeltaChange(old.getValue(i), new.getValue(i))
                        if (out.isChange())
                            block(k toComparable i, out)
                    }


                    // Produce all adds.
                    for (i in new.keys subtract old.keys)
                        block(k toComparable i, DeltaAdd(new.getValue(i)))
                }
            }
        }
    }

    override fun put(key: ComparablePair<K, Int>, value: V): V? {
        // Find the source list, if not present, return null.
        val (_, vs) = it.find(At(key.first)).singleOrNull()
            ?: return null

        // If OOB, return null.
        if (vs.size <= key.second)
            return null

        // Compute new parts, replace the item at the position.
        val head = vs.asSequence().take(key.second)
        val tail = vs.asSequence().drop(key.second + 1)
        val new = (head + value + tail).toList()

        // Add new value to source.
        it.put(key.first, new)

        // Return what was in the old list.
        return vs[key.second]
    }

    override fun remove(key: ComparablePair<K, Int>): V? {
        // Find the source list, if not present, return null.
        val (_, vs) = it.find(At(key.first)).singleOrNull()
            ?: return null

        // If OOB, return null.
        if (vs.size <= key.second)
            return null

        // Compute new parts, drop item at position.
        val head = vs.asSequence().take(key.second)
        val tail = vs.asSequence().drop(key.second + 1)
        val new = (head + tail).toList()

        // Add new value to source.
        it.put(key.first, new)

        // Return what was in the old list.
        return vs[key.second]
    }

    override fun find(query: Query<ComparablePair<K, Int>>): Sequence<Pair<ComparablePair<K, Int>, V>> {
        // Split query.
        val (queryLeft, queryRight) = split(query)

        // Find left part of the query in source.
        return it.find(queryLeft).flatMap { (k, vs) ->
            // Flat map the results of those, include index, filter with the second part of the query.
            vs.asSequence().mapIndexed { i, v -> (k toComparable i) to v }
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
 * Creates a [Select] index with the given args that is than flattened.
 */
fun <K : Comparable<K>, V, R> Index<K, V>.selectMany(deselector: (List<R>) -> V, selector: (V) -> List<R>) =
    Flatten(Select(this, deselector, selector))

/**
 * Creates a [Select] index with the given args that is than flattened.
 */
fun <K : Comparable<K>, V, R> Index<K, V>.selectMany(selector: (V) -> List<R>) =
    Flatten(Select(this, selector))