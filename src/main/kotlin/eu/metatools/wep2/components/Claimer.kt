package eu.metatools.wep2.components

import eu.metatools.wep2.entity.RestoringEntity
import eu.metatools.wep2.storage.Store
import eu.metatools.wep2.tools.ReclaimableSequence
import eu.metatools.wep2.track.Claimer
import eu.metatools.wep2.track.undos
import eu.metatools.wep2.util.*


/**
 * Delegates as a claimer, claiming elements from the [sequence], with the given [zero] and [inc] recycle counts.
 */
fun <I : Comparable<I>, R : Comparable<R>> claimer(sequence: Sequence<I>, zero: R, inc: (R) -> R) =
    @Suppress("unchecked_cast")
    ReadOnlyPropertyProvider { thisRef: Any?, property ->
        // Get reference and see if it is a restoring entity.
        val asRestoring = thisRef as? RestoringEntity<*, *, *>

        // Load or initialize the sequence.
        val source = asRestoring?.restore?.let {
            // Restoring, load values.
            val (head, recycled) = it.load<Pair<I?, List<ComparablePair<I, R>>>>(property.name)
            ReclaimableSequence.restore(sequence, zero, inc, head, recycled)
        } ?: run {
            // Not restoring, initialize new.
            ReclaimableSequence(sequence, zero, inc)
        }

        // Amend save if in restoring entity.
        asRestoring?.saveWith({ store: Store ->
            // Save as head and recycled elements.
            store.save(property.name, source.generatorHead to source.recycled)
        } labeledAs {
            "save claimer ${property.name}"
        })

        // Return direct property on a claimer on the source.
        Property(Claimer(source))
    }

/**
 * Delegates as a claimer, uses short as recycle counts.
 */
fun <I : Comparable<I>> claimer(sequence: Sequence<I>) =
    claimer(sequence, 0.toShort(), Short::inc)