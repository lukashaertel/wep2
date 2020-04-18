package eu.metatools.sx.index

/**
 * Creates a [Select] index with the given args that is than flattened. Allows putting back to out of bounds
 * indices, using the given [zero] element to pad the intermediate positions.
 */
fun <K : Comparable<K>, V, R> Index<K, V>.selectMany(zero: () -> R, inv: (List<R>) -> V, block: (V) -> List<R>) =
    Flatten(Select(this, inv, block), zero)

/**
 * Creates a [Select] index with the given args that is than flattened. Allows putting back into the original lists and
 * at their ends. Out of bounds puts are not supported and require a `zero`.
 */
fun <K : Comparable<K>, V, R> Index<K, V>.selectMany(inv: (List<R>) -> V, block: (V) -> List<R>) =
    Flatten(Select(this, inv, block))

/**
 * Creates a [Select] index with the given args that is than flattened. Putting back is not supported.
 */
fun <K : Comparable<K>, V, R> Index<K, V>.selectMany(block: (V) -> List<R>) =
    Flatten(Select(this, block))