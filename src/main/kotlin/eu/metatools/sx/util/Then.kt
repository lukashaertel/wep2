package eu.metatools.sx.util

data class Then<T : Comparable<T>, U : Comparable<U>>(
    val first: T,
    val second: U
) : Comparable<Then<T, U>> {
    override fun compareTo(other: Then<T, U>): Int {
        val r = first.compareTo(other.first)
        if (r != 0) return r
        return second.compareTo(other.second)
    }

    override fun toString() = "`($first, $second)"
}

infix fun <T : Comparable<T>, U : Comparable<U>> T.then(second: U) =
    Then(this, second)