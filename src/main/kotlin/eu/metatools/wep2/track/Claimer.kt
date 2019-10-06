package eu.metatools.wep2.track

import eu.metatools.wep2.tools.ReclaimableSequence
import eu.metatools.wep2.util.ComparablePair
import eu.metatools.wep2.util.labeledAs
import eu.metatools.wep2.util.uv
import eu.metatools.wep2.util.within

/**
 * Wraps generating elements of a [ReclaimableSequence] providing automated undo.
 */
class Claimer<I : Comparable<I>, R : Comparable<R>>(val reclaimableSequence: ReclaimableSequence<I, R>) {
    /**
     * Claims a value.
     */
    fun claim(): ComparablePair<I, R> {
        val (value, undo) = reclaimableSequence.claim()
        undos.get()?.add(undo labeledAs {
            "releasing $value"
        })
        return value
    }

    /**
     * Releases a value.
     */
    fun release(value: ComparablePair<I, R>) {
        val undo = reclaimableSequence.release(value)
        undos.get()?.add(undo labeledAs {
            "reclaiming $value"
        })
    }
}

/**
 * Claims a value of a [Claimer], returns the first component without the recycle count.
 */
fun <I : Comparable<I>> Claimer<I, *>.claimValue() =
    claim().first

/**
 * Claims a random int with the given bounds.
 *
 * Shorthand for `claimValue().within(lower, upper)`.
 */
fun Claimer<Int, *>.randomInt(lower: Int, upper: Int, upperInclusive: Boolean = false) =
    claimValue().within(lower, if (upperInclusive) upper + 1 else upper)

/**
 * Claims a random double in [[0.0, 1.0]].
 *
 * Shorthand for `claimValue().uv()`.
 */
fun Claimer<Int, *>.randomDouble() =
    claimValue().uv()

/**
 * Gets a random element of the list.
 */
fun <E> Claimer<Int, *>.randomOf(list: List<E>) =
    list[randomInt(0, list.size)]

/**
 * Gets a random element of the array.
 */
inline fun <reified E> Claimer<Int, *>.randomOf(array: Array<E>) =
    array[randomInt(0, array.size)]

/**
 * Gets a random element of the map, keys must be sortable.
 */
fun <K : Comparable<K>, V> Claimer<Int, *>.randomOf(map: Map<K, V>) =
    map[randomOf(map.keys.sorted())]

