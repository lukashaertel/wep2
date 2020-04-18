package eu.metatools.sx.index

/**
 * Computes the union of the indices [left] and [right]. The union is strict in that if key assignments differ between
 * both operands, the result will contain neither. Put and remove operations are maximal as they will put to and remove
 * from both operands, regardless of adding to one alone already mutating the output.
 * @property left The left index.
 * @property right The right index.
 */
data class Union<K : Comparable<K>, V>(
    val left: Index<K, V>,
    val right: Index<K, V>
) : Index<K, V>() {
    override fun register(query: Query<K>, block: (K, Delta<V>) -> Unit): AutoCloseable {
        // Register listener for left.
        val leftListener = left.register(query) { key, d ->
            // For all changes on left, pair with matching right elements.
            val coElement = right.find(At(key)).singleOrNull()

            when (d) {
                is DeltaAdd ->
                    if (coElement == null)
                    // Not assigned in other, run add.
                        block(key, d)
                    else if (coElement.second != d.added)
                    // Assigned differently in other, run as remove.
                        block(key, DeltaRemove(coElement.second))

                is DeltaRemove ->
                    if (coElement == null)
                    // not assigned in other, run remove.
                        block(key, d)
                    else if (coElement.second != d.removed)
                    // Assigned differently in other, run as add.
                        block(key, DeltaAdd(coElement.second))

                is DeltaChange ->
                    if (coElement == null)
                        block(key, d)
                    else if (coElement.second == d.from)
                        block(key, DeltaRemove(coElement.second))
                    else if (coElement.second == d.to)
                        block(key, DeltaAdd(coElement.second))
            }
        }

        // Register listener for right.
        val rightListener = right.register(query) { key, d ->
            // For all changes on left, pair with matching right elements.
            val coElement = left.find(At(key)).singleOrNull()

            when (d) {
                is DeltaAdd ->
                    if (coElement == null)
                    // Not assigned in other, run add.
                        block(key, d)
                    else if (coElement.second != d.added)
                    // Assigned differently in other, run as remove.
                        block(key, DeltaRemove(coElement.second))

                is DeltaRemove ->
                    if (coElement == null)
                    // not assigned in other, run remove.
                        block(key, d)
                    else if (coElement.second != d.removed)
                    // Assigned differently in other, run as add.
                        block(key, DeltaAdd(coElement.second))

                is DeltaChange ->
                    if (coElement == null)
                        block(key, d)
                    else if (coElement.second == d.from)
                        block(key, DeltaRemove(coElement.second))
                    else if (coElement.second == d.to)
                        block(key, DeltaAdd(coElement.second))
            }
        }

        // On close, remove both part listeners.
        return AutoCloseable {
            leftListener.close()
            rightListener.close()
        }
    }

    override fun put(key: K, value: V): V? {
        if (left === right)
            return left.put(key, value)

        val fromLeft = left.put(key, value)
        val fromRight = right.put(key, value)
        return if (fromLeft == fromRight)
            fromLeft
        else
            null

    }

    override fun remove(key: K): V? {
        if (left === right)
            return left.remove(key)


        val fromLeft = left.remove(key)
        val fromRight = right.remove(key)
        return if (fromLeft == fromRight)
            fromLeft
        else
            null
    }

    override fun find(query: Query<K>): Sequence<Pair<K, V>> {
        if (left === right)
            return left.find(query)

        return left.find(query).filter { (key, value) ->
            right.find(At(key)).singleOrNull() == value
        }
    }
}

/**
 * Creates a union with the operands.
 */
infix fun <K : Comparable<K>, V> Index<K, V>.union(other: Index<K, V>) =
    Union(this, other)

// (TODO Comparable --> Comparator)


//
//fun <K : Comparable<K>, V> reachAll(
//    index: Index<K, V>, hops: Int,
//    next: (K) -> Query<K>,
//    before: (K) -> Query<K>,
//    min: K, max: K
//): Index<ComparableList<K>, List<V>> {
//    // TODO: Union/Reindex still fucked.
//    // Start with zero hops, make union with all other hops.
//    return (1..hops).fold(reach(index, 0, next, before, min, max)) { c, n ->
//        c union reach(index, n, next, before, min, max)
//    }
//}
