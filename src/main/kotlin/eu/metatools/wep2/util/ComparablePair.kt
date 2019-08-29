package eu.metatools.wep2.util

/**
 * A pair of entries that are comparable and that is itself
 * comparable, prioritizing the first element.
 * @property first The first or left element.
 * @property second The second or right element.
 */
data class ComparablePair<T : Comparable<T>, U : Comparable<U>>(
    val first: T,
    val second: U
) : Comparable<ComparablePair<T, U>> {
    override fun compareTo(other: ComparablePair<T, U>): Int {
        // Compare first, return if non-zero.
        val compareFirst = first.compareTo(other.first)
        if (compareFirst != 0)
            return compareFirst

        // Compare second, return if non-zero.
        val compareSecond = second.compareTo(other.second)
        if (compareSecond != 0)
            return compareSecond

        // Equal, return zero.
        return 0
    }

    override fun toString() = "($first, $second)"
}

/**
 * Creates a comparable pair of the receiver and [second].
 */
infix fun <T : Comparable<T>, U : Comparable<U>> T.toComparable(second: U) =
    ComparablePair(this, second)