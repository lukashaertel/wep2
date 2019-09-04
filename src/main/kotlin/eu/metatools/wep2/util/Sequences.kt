package eu.metatools.wep2.util

import java.util.*

/**
 * Sequence of short natural numbers.
 */
fun shorts(start: Short = 0) =
    generateSequence(start, Short::inc)

/**
 * Sequence of long natural numbers.
 */
fun longs(start: Long = 0L) =
    generateSequence(start, Long::inc)

/**
 * Sequence of random integers.
 */
fun randomInts(seed: Long = 0L): Sequence<Int> {
    val random = Random(seed)
    return generateSequence { random.nextInt() }
}