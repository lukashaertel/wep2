package eu.metatools.wep2.components

import eu.metatools.wep2.aspects.Restoring
import eu.metatools.wep2.aspects.Saving
import eu.metatools.wep2.storage.Store
import eu.metatools.wep2.tools.ScopedSequence
import eu.metatools.wep2.tools.TimeGenerator
import eu.metatools.wep2.util.delegates.Property
import eu.metatools.wep2.util.delegates.ReadOnlyPropertyProvider
import eu.metatools.wep2.util.labeledAs

/**
 * Delegates as a timer, using standard parameters and (re)storing scopes if in the proper aspects.
 */
fun timer() =
    ReadOnlyPropertyProvider { thisRef: Any?, property ->
        // Get aspects of the receiver.
        val restoring = thisRef as? Restoring
        val saving = thisRef as? Saving


        // Load or initialize the sequence.
        val source = restoring?.restore?.let {
            // Restoring, load scopes.
            val scopes = it.load<Map<Long, Byte>>(property.name)

            ScopedSequence.restore(TimeGenerator.defaultLocalIDs, scopes)
        } ?: run {
            // Not restoring, initialize new.
            ScopedSequence<Long, Byte>(TimeGenerator.defaultLocalIDs)
        }

        // Amend save if in appropriate aspect.
        saving?.saveWith({ store: Store ->
            // Save as scopes.
            store.save(property.name, source.scopes)
        } labeledAs {
            "save timer ${property.name}"
        })

        // Return direct property on a time generator the source.
        Property(TimeGenerator(source))
    }