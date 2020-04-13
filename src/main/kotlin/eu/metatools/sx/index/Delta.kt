package eu.metatools.sx.index

/**
 * Compound delta type, see [DeltaAdd], [DeltaRemove], [DeltaChange].
 * @param V The type of the values.
 */
sealed class Delta<V> {
    /**
     * True if a delta is a change. On the frontend, as a result of registering a
     * change listener, this should always be true.
     */
    abstract fun isChange(): Boolean
}

/**
 * Addition of a value [added].
 */
data class DeltaAdd<V>(val added: V) : Delta<V>() {
    override fun isChange() = true

    override fun toString() = "(* ~> $added)"
}

/**
 * Removal of a value [removed].
 */
data class DeltaRemove<V>(val removed: V) : Delta<V>() {
    override fun isChange() = true

    override fun toString() = "($removed ~> *)"
}

/**
 * Change of the value from [from] to [to].
 */
data class DeltaChange<V>(val from: V, val to: V) : Delta<V>() {
    override fun isChange() = from != to

    override fun toString() = "($from ~> $to)"
}

/**
 * Maps the values that are part of [DeltaAdd], [DeltaRemove], [DeltaChange].
 */
inline fun <V1, V2> Delta<V1>.map(block: (V1) -> V2) =
    when (this) {
        is DeltaAdd -> DeltaAdd(block(added))
        is DeltaRemove -> DeltaRemove(block(removed))
        is DeltaChange -> DeltaChange(block(from), block(to))
    }