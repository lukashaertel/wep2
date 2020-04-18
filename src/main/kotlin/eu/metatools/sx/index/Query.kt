package eu.metatools.sx.index

import eu.metatools.sx.util.Then

/**
 * A basic query on comparable keys.
 */
sealed class Query<K : Comparable<K>> : (K) -> Boolean {
    companion object {
        /**
         * Reduces a list of queries by a reducing via [merge].
         * @param queries The queries to reduce.
         * @return Returns a reduced query.
         */
        fun <K : Comparable<K>> reduce(vararg queries: Query<K>) =
            queries.reduce { l, r -> l.merge(r) }
    }

    /**
     * Merges this query with another for a resulting query.
     */
    abstract fun merge(other: Query<K>): Query<K>

    abstract fun <T : Comparable<T>> map(transform: (K) -> T): Query<T>
}

/**
 * A query that always returns true.
 */
class Always<K : Comparable<K>>() : Query<K>() {
    override fun invoke(p1: K) =
        true

    override fun merge(other: Query<K>) =
        other

    override fun toString() =
        "true"

    override fun <T : Comparable<T>> map(transform: (K) -> T) =
        Always<T>()
}

/**
 * A query that never returns true.
 */
class Never<K : Comparable<K>>() : Query<K>() {
    override fun invoke(p1: K) =
        false

    override fun merge(other: Query<K>) =
        this

    override fun toString() =
        "false"

    override fun <T : Comparable<T>> map(transform: (K) -> T) =
        Never<T>()
}

/**
 * A query on an exact value, only returns true if the candidate has
 * the given [key].
 * @property key The only key that matches.
 */
data class At<K : Comparable<K>>(val key: K) : Query<K>() {
    override fun invoke(p1: K) =
        p1 == key

    override fun merge(other: Query<K>) =
        when (other) {
            is Always<K> -> this
            is Never<K> -> Never<K>()
            is At<K> -> if (key == other.key) this else Never<K>()
            is After<K> -> if (other(key)) this else Never<K>()
            is Before<K> -> if (other(key)) this else Never<K>()
            is Between<K> -> if (other(key)) this else Never<K>()
        }

    override fun toString() =
        "[$key]"

    override fun <T : Comparable<T>> map(transform: (K) -> T) =
        At(transform(key))
}

/**
 * A query on a lower boundary, returns true if the candidate has
 * a key greater than or equal to [key].
 * @property key The lower boundary, inclusive.
 */
data class After<K : Comparable<K>>(val key: K) : Query<K>() {
    override fun invoke(p1: K) =
        key <= p1


    override fun merge(other: Query<K>) =
        when (other) {
            is Always<K> -> this
            is Never<K> -> Never<K>()
            is At<K> -> if (invoke(other.key)) other else Never<K>()
            is After<K> -> if (key < other.key) other else this
            is Before<K> -> if (key < other.key) Between(key, other.key) else Never<K>()
            is Between<K> -> if (other(key)) Between(key, other.keyUpper) else Never<K>()
        }

    override fun toString() =
        "[$key,]"

    override fun <T : Comparable<T>> map(transform: (K) -> T) =
        After(transform(key))
}

/**
 * A query on an upper boundary, returns true if the candidate has
 * a key less than or equal to [key].
 * @property key The upper boundary, exclusive.
 */
data class Before<K : Comparable<K>>(val key: K) : Query<K>() {
    override fun invoke(p1: K) =
        p1 <= key

    override fun merge(other: Query<K>) =
        when (other) {
            is Always<K> -> this
            is Never<K> -> Never<K>()
            is At<K> -> if (invoke(other.key)) other else Never<K>()
            is After<K> -> if (key > other.key) Between(other.key, key) else Never<K>()
            is Before<K> -> if (key < other.key) this else other
            is Between<K> -> if (other(key)) Between(other.keyLower, key) else Never<K>()
        }

    override fun toString() =
        "[,$key]"

    override fun <T : Comparable<T>> map(transform: (K) -> T) =
        Before(transform(key))
}

/**
 * A query on a range, returns tre if the candidate has a key that
 * is greater than or equal to [keyLower] and less than or equal to [keyUpper].
 * @property keyLower The lower boundary, inclusive.
 * @property keyUpper The upper boundary, inclusive.
 */
data class Between<K : Comparable<K>>(
    val keyLower: K,
    val keyUpper: K
) : Query<K>() {
    override fun invoke(p1: K) =
        p1 in keyLower..keyUpper

    override fun merge(other: Query<K>) =
        when (other) {
            is Always<K> -> this
            is Never<K> -> Never<K>()
            is At<K> -> if (invoke(other.key)) other else Never<K>()
            is After<K> -> if (invoke(other.key)) Between(other.key, keyUpper) else Never<K>()
            is Before<K> -> if (invoke(other.key)) Between(keyLower, other.key) else Never<K>()
            is Between<K> -> {
                val nextLower = maxOf(keyLower, other.keyLower)
                val nextUpper = minOf(keyUpper, other.keyUpper)
                if (nextLower < nextUpper)
                    Between(nextLower, nextUpper)
                else
                    Never<K>()
            }
        }

    override fun toString() =
        "[$keyLower, $keyUpper]"

    override fun <T : Comparable<T>> map(transform: (K) -> T) =
        Between(transform(keyLower), transform(keyUpper))
}

/**
 * Splits a query into parts applicable to [left] and [right]
 */
fun <K1 : Comparable<K1>, K2 : Comparable<K2>> split(query: Query<Then<K1, K2>>) =
    when (query) {
        is Always -> Always<K1>() to Always<K2>()
        is Never -> Never<K1>() to Never<K2>()
        is At -> At(query.key.first) to At(query.key.second)
        is After -> After(query.key.first) to After(query.key.second)
        is Before -> Before(query.key.first) to Before(query.key.second)
        is Between -> Between(query.keyLower.first, query.keyUpper.first) to
                Between(query.keyLower.second, query.keyUpper.second)
    }