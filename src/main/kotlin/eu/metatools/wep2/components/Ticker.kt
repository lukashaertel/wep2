package eu.metatools.wep2.components

import eu.metatools.wep2.aspects.Restoring
import eu.metatools.wep2.aspects.Saving
import eu.metatools.wep2.storage.Store
import eu.metatools.wep2.tools.TickGenerator
import eu.metatools.wep2.util.delegates.Property
import eu.metatools.wep2.util.delegates.ReadOnlyPropertyProvider
import eu.metatools.wep2.util.labeledAs

/**
 * Delegates as a ticker, ticks in the given [frequency] starting at [initial].
 */
fun ticker(frequency: Long, initial: Long) =
    @Suppress("unchecked_cast")
    (ReadOnlyPropertyProvider { thisRef: Any?, property ->
        // Get aspects of the receiver.
        val restoring = thisRef as? Restoring
        val saving = thisRef as? Saving

        // Load or initialize the sequence.
        val source = restoring?.restore?.let {
            // Restoring, load last time.
            val lastTime = it.load<Long>(property.name)

            TickGenerator.restore(initial, frequency, lastTime)
        } ?: run {
            // Not restoring, initialize new.
            TickGenerator(initial, frequency)
        }

        // Amend save if in appropriate aspect.
        saving?.saveWith({ store: Store ->
            // Save as last ticked time.
            store.save(property.name, source.lastTime)
        } labeledAs {
            "save ticker ${property.name}"
        })

        // Return direct property on the source.
        Property(source)
    })