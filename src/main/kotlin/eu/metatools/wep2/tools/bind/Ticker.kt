package eu.metatools.wep2.tools.bind

import eu.metatools.wep2.entity.RestoringEntity
import eu.metatools.wep2.entity.bind.Restore
import eu.metatools.wep2.entity.bind.Store
import eu.metatools.wep2.lang.DirectValue
import eu.metatools.wep2.lang.ReadOnlyPropertyProvider
import eu.metatools.wep2.tools.TickGenerator
import eu.metatools.wep2.track.bind.saveSet
import eu.metatools.wep2.util.SimpleSet
import eu.metatools.wep2.util.labeledAs
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1

/**
 * A tick generator partaking in change based undo-tracking.
 */
fun ticker(restore: Restore?, frequency: Long, initial: () -> Long) =
    // Create a property provider to receive the name.
    ReadOnlyPropertyProvider { thisRef: Any?, property ->
        // Apply auto-saving mechanism.
        if (thisRef is RestoringEntity<*, *, *>) {
            // Type assert on result type, receiver is not null so this is a member property.
            @Suppress("unchecked_cast")
            property as KProperty1<Any?, TickGenerator>

            thisRef.saveWith({ store: Store ->
                store.saveTicker(property.name, property.get(thisRef))
            } labeledAs { "Save set ${property.name}" })
        }

        // Check if currently restoring.
        if (restore != null) {
            // Restoring, load last time.
            val (initialTime, lastTime) = restore.load<Pair<Long, Long>>(property.name)

            // Return ticker restore value.
            DirectValue(TickGenerator.restore(initialTime, frequency, lastTime))
        } else {
            // Otherwise, just create new ticker.
            DirectValue(TickGenerator(initial(), frequency))
        }
    }

/**
 * Saves the value of the property with a [TickGenerator] to the receiver.
 */
fun Store.saveTicker(name: String, value: TickGenerator) {
    save(name, value.initial to value.lastTime)
}

/**
 * Saves the value of the property with a [TickGenerator] to the receiver.
 */
fun Store.saveTicker(property: KProperty0<TickGenerator>) =
    saveTicker(property.name, property.get())