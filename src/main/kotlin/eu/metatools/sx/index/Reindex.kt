package eu.metatools.sx.index

/**
 * Reindexes the keys of [it] with the bidirectional mapping [selector] and [deselector].
 */
data class Reindex<K1 : Comparable<K1>, K2 : Comparable<K2>, V>(
    val it: Index<K1, V>,
    val deselector: (K2) -> K1,
    val selector: (K1) -> K2
) : Index<K2, V>() {
    override fun register(query: Query<K2>, block: (K2, Delta<V>) -> Unit): AutoCloseable {
        return it.register(query.map(deselector)) { k, d ->
            block(selector(k), d)
        }
    }

    override fun put(key: K2, value: V) =
        it.put(deselector(key), value)

    override fun remove(key: K2) =
        it.remove(deselector(key))

    override fun find(query: Query<K2>): Sequence<Pair<K2, V>> {
        return it.find(query.map(deselector)).map { (k, v) ->
            selector(k) to v
        }
    }

    override fun toString() =
        "$it[key -> $selector(key)]"
}


/**
 * Creates a [Reindex] with the given args.
 */
fun <K1 : Comparable<K1>, K2 : Comparable<K2>, V> Index<K1, V>.reindex(deselector: (K2) -> K1, selector: (K1) -> K2) =
    Reindex(this, deselector, selector)