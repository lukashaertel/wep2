package eu.metatools.wep2.track.bind

import eu.metatools.wep2.entity.RestoringEntity
import eu.metatools.wep2.entity.bind.Restore
import eu.metatools.wep2.entity.bind.Store
import eu.metatools.wep2.lang.DirectValue
import eu.metatools.wep2.lang.ReadOnlyPropertyProvider
import eu.metatools.wep2.tools.ReclaimableSequence
import eu.metatools.wep2.track.Claimer
import eu.metatools.wep2.util.SimpleSet
import eu.metatools.wep2.util.labeledAs
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1

/**
 * Stores a claimer, provides restoration.
 */
fun <I, R> claimer(restore: Restore?, sequence: Sequence<I>, zero: R, inc: (R) -> R) =
    // Create a property provider to receive the name.
    ReadOnlyPropertyProvider { thisRef: Any?, property ->
        // Apply auto-saving mechanism.
        if (thisRef is RestoringEntity<*, *, *>) {
            // Type assert on result type, receiver is not null so this is a member property.
            @Suppress("unchecked_cast")
            property as KProperty1<Any?, Claimer<I, R>>

            thisRef.saveWith({ store: Store ->
                store.saveClaimer(property.name, property.get(thisRef))
            } labeledAs { "Save claimer ${property.name}" })
        }

        // Check if currently restoring.
        if (restore != null) {
            // Restoring, load values.
            val (head, recycled) = restore.load<Pair<I?, List<Pair<I, R>>>>(property.name)

            // Return claimer with values.
            DirectValue(Claimer(ReclaimableSequence.restore(sequence, zero, inc, head, recycled)))
        } else {
            // Otherwise, just create new claimer.
            DirectValue(Claimer(ReclaimableSequence(sequence, zero, inc)))
        }
    }

/**
 * Creates a [claimer] with short recycle counts.
 */
fun <I> claimer(restore: Restore?, sequence: Sequence<I>) =
    claimer(restore, sequence, 0, Short::inc)

/**
 * Saves the value of the property with a [SimpleSet] of entity references to the receiver.
 */
fun Store.saveClaimer(name: String, value: Claimer<*, *>) {
    save(name, value.reclaimableSequence.generatorHead to value.reclaimableSequence.recycled)
}

/**
 * Saves the value of the property with a [SimpleSet] of entity references to the receiver.
 */
fun Store.saveClaimer(property: KProperty0<Claimer<*, *>>) {
    saveClaimer(property.name, property.get())
}