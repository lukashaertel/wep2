package eu.metatools.wep2.util

/**
 * Returns the positive modulus. Computed as `((i % mod) + mod) % mod`.
 */
fun Int.modPos(mod: Int) =
    ((this % mod) + mod) % mod

/**
 * Shifts this integer to be within [[lower], [upper]). Computed as `lower + i.`[modPos]`(upper - lower)`
 */
fun Int.within(lower: Int, upper: Int) =
    lower + this.modPos(upper - lower)

/**
 * Returns the position of the integer in [[Int.MIN_VALUE], [Int.MAX_VALUE]] as [[0.0, 1.0]]
 */
fun Int.uv() =
    let {
        (it.toLong() - Int.MIN_VALUE).toDouble() /
                (Int.MAX_VALUE.toLong() - Int.MIN_VALUE - 1L)
    }
