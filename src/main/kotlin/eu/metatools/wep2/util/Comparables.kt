package eu.metatools.wep2.util

import java.io.Serializable

/**
 * Returns equal if receiver is reference equal to [other]. If not, returns the result of the first comparison.
 */
fun <T> T.first(other: T, block: () -> Int): Int {
    if (this === other) return 0
    return block()
}

/**
 * If the receiver is zero, invokes the block.
 */
inline infix fun Int.then(block: () -> Int): Int {
    if (this != 0)
        return this

    return block()
}

/**
 * A pair of two comparable entries, itself comparable by comparing [first], then [second].
 */
data class ComparablePair<T : Comparable<T>, U : Comparable<U>>(
    val first: T, val second: U
) : Comparable<ComparablePair<T, U>>, Serializable {
    override fun compareTo(other: ComparablePair<T, U>) =
        first(other) {
            first.compareTo(other.first)
        } then {
            second.compareTo(other.second)
        }

    override fun toString() =
        "($first, $second)"
}