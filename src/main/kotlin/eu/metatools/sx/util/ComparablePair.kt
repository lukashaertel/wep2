package eu.metatools.sx.util

data class ComparablePair<T : Comparable<T>, U : Comparable<U>>(
    val first: T,
    val second: U
) : Comparable<ComparablePair<T, U>> {
    override fun compareTo(other: ComparablePair<T, U>): Int {
        val r = first.compareTo(other.first)
        if (r != 0) return r
        return second.compareTo(other.second)
    }

    override fun toString() = "`($first, $second)"
}

infix fun <T : Comparable<T>, U : Comparable<U>> T.toComparable(second: U) =
    ComparablePair(this, second)