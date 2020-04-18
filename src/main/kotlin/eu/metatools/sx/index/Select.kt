package eu.metatools.sx.index

/**
 * Transforms the items of [it] by applying the [block].
 * @property it The source index.
 * @property inv If given, allows putting an element in this index by putting the deselected element into
 * the source index.
 * @property block The block to apply on incoming elements.
 */
data class Select<K : Comparable<K>, V, R>(
    val it: Index<K, V>,
    val inv: ((R) -> V)?,
    val block: (V) -> R
) : Index<K, R>() {
    /**
     * The deselector instance, if no deselector is given, this will default to an instance
     * that throws an unsupported operation exception.
     */
    private val invInstance = inv ?: {
        throw UnsupportedOperationException("Selection is not bi-directional on index $it")
    }

    /**
     * Constructs a select index without reverse mutability. Used when the selection is not
     * @param it The source index.
     * @param selector The block to apply on incoming elements.
     */
    constructor(it: Index<K, V>, selector: (V) -> R) : this(it, null, selector)

    override fun register(query: Query<K>, block: (K, Delta<R>) -> Unit) =
        it.register(query) { k, d ->
            val out = d.map(this.block)
            if (out.isChange())
                block(k, out)
        }

    override fun put(key: K, value: R) =
        it.put(key, invInstance(value))?.let(block)

    override fun remove(key: K) =
        it.remove(key)?.let(block)

    override fun find(query: Query<K>) =
        it.find(query).map { (k, v) ->
            k to block(v)
        }

    override fun toString() =
        "$it[value -> $block(value)]"
}

/**
 * Creates a [Select] index with the given args.
 */
fun <K : Comparable<K>, V, R> Index<K, V>.select(deselector: (R) -> V, selector: (V) -> R) =
    Select(this, deselector, selector)

/**
 * Creates a [Select] index with the given args.
 */
fun <K : Comparable<K>, V, R> Index<K, V>.select(selector: (V) -> R) =
    Select(this, selector)