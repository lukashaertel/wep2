package eu.metatools.sx.index

/**
 * Limits the assignments produced by [it] to those satisfying the [predicate].
 * @property it The source index.
 * @property predicate The predicate to apply on the assignments.
 */
data class Where<K : Comparable<K>, V>(
    val it: Index<K, V>,
    val predicate: (K, V) -> Boolean
) : Index<K, V>() {
    override fun register(query: Query<K>, block: (K, Delta<V>) -> Unit): AutoCloseable {
        // Register a listener on the source that filters with the predicate.
        return it.register(query) { k, d ->
            when (d) {
                // Guard single item predicates by predicate.
                is DeltaAdd -> if (predicate(k, d.added)) block(k, d)
                is DeltaRemove -> if (predicate(k, d.removed)) block(k, d)

                // For change, see if translation to remove or add is needed.
                is DeltaChange -> {
                    val pFrom = predicate(k, d.from)
                    val pTo = predicate(k, d.to)
                    if (pFrom && pTo)
                        block(k, d)
                    else if (pFrom)
                        block(k, DeltaRemove(d.from))
                    else if (pTo)
                        block(k, DeltaAdd(d.to))
                }
            }
        }
    }

    override fun put(key: K, value: V) =
        // Delegate to the source.
        it.put(key, value)

    override fun remove(key: K) =
        // Delegate to the source.
        it.remove(key)

    override fun find(query: Query<K>): Sequence<Pair<K, V>> {
        // Find all satisfying assignments and filter them with the predicate.
        return it.find(query).filter { (k, v) -> predicate(k, v) }
    }
}

/**
 * Creates a [Where] index with the given predicate.
 */
fun <K : Comparable<K>, V> Index<K, V>.where(predicate: (K, V) -> Boolean) =
    Where(this, predicate)