package eu.metatools.wep2.components

import eu.metatools.wep2.aspects.Restoring
import eu.metatools.wep2.aspects.Saving
import eu.metatools.wep2.storage.Store
import eu.metatools.wep2.tools.ReclaimableSequence
import eu.metatools.wep2.track.undos
import eu.metatools.wep2.util.ComparablePair
import eu.metatools.wep2.util.delegates.Property
import eu.metatools.wep2.util.delegates.ReadOnlyPropertyProvider
import eu.metatools.wep2.util.labeledAs
import eu.metatools.wep2.util.uv
import eu.metatools.wep2.util.within


/**
 * Delegates as a claimer, claiming elements from the [sequence], with the given [zero] and [inc] recycle counts.
 */
fun <I : Comparable<I>, R : Comparable<R>> claimer(sequence: Sequence<I>, zero: R, inc: (R) -> R) =
    @Suppress("unchecked_cast")
    ReadOnlyPropertyProvider { thisRef: Any?, property ->
        // Get aspects of the receiver.
        val restoring = thisRef as? Restoring
        val saving = thisRef as? Saving

        // Load or initialize the sequence.
        val source = restoring?.restore?.let {
            // Restoring, load values.
            val (head, recycled) = it.load<Pair<I?, List<ComparablePair<I, R>>>>(property.name)
            ReclaimableSequence.restore(sequence, zero, inc, head, recycled)
        } ?: run {
            // Not restoring, initialize new.
            ReclaimableSequence(sequence, zero, inc)
        }

        // Amend save if in appropriate aspect.
        saving?.saveWith({ store: Store ->
            // Save as head and recycled elements.
            store.save(property.name, source.generatorHead to source.recycled)
        } labeledAs {
            "save claimer ${property.name}"
        })

        // Return direct property on a claimer on the source.
        Property<Claimer<I, R>>(object : Claimer<I, R> {
            /**
             * Claims a value.
             */
            override fun claim(): ComparablePair<I, R> {
                val (value, undo) = source.claim()
                undos.get()?.add(undo labeledAs {
                    "releasing $value"
                })
                return value
            }

            /**
             * Releases a value.
             */
            override fun release(value: ComparablePair<I, R>) {
                val undo = source.release(value)
                undos.get()?.add(undo labeledAs {
                    "reclaiming $value"
                })
            }
        })
    }

/**
 * Delegates as a claimer, uses short as recycle counts.
 */
fun <I : Comparable<I>> claimer(sequence: Sequence<I>) =
    claimer(sequence, 0.toShort(), Short::inc)

interface Claimer<I : Comparable<I>, R : Comparable<R>> {
    /**
     * Claims a value.
     */
    fun claim(): ComparablePair<I, R>

    /**
     * Releases a value.
     */
    fun release(value: ComparablePair<I, R>)
}


/**
 * Claims a value of a [ClaimerImpl], returns the first component without the recycle count.
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