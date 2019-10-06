package eu.metatools.wep2.components

import eu.metatools.wep2.entity.RestoringEntity
import eu.metatools.wep2.storage.Store
import eu.metatools.wep2.tools.TickGenerator
import eu.metatools.wep2.util.Property
import eu.metatools.wep2.util.ReadOnlyPropertyProvider
import eu.metatools.wep2.util.labeledAs

/**
 * Delegates as a ticker, ticks in the given [frequency] starting at [initial].
 */
fun ticker(frequency: Long, initial: Long) =
    @Suppress("unchecked_cast")
    (ReadOnlyPropertyProvider { thisRef: Any?, property ->
        // Get reference and see if it is a restoring entity.
        val asRestoring = thisRef as? RestoringEntity<*, *, *>

        // Load or initialize the sequence.
        val source = asRestoring?.restore?.let {
            // Restoring, load last time.
            val lastTime = it.load<Long>(property.name)

            TickGenerator.restore(initial, frequency, lastTime)
        } ?: run {
            // Not restoring, initialize new.
            TickGenerator(initial, frequency)
        }

        // Amend save if in restoring entity.
        asRestoring?.saveWith({ store: Store ->
            // Save as last ticked time.
            store.save(property.name, source.lastTime)
        } labeledAs {
            "save ticker ${property.name}"
        })

        // Return direct property on the source.
        Property(source)
    })