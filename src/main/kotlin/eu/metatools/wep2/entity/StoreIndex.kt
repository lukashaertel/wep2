package eu.metatools.wep2.entity

import eu.metatools.wep2.storage.Store
import eu.metatools.wep2.storage.path

/**
 * Stores all entities in the index of the [context] to the [store] object. The store will be filled with an
 * `"allEntities"` entry memorizing the IDs and the classes of the entities via their primary constructors.
 *
 * @param context The context to store from.
 * @param store The store object.
 */
fun <N, T : Comparable<T>, I : Comparable<I>> storeIndex(
    context: Context<N, T, I>,
    store: Store
) {
    // Create the allEntities field from IDs and primary constructors.
    val constructors = context.index.mapNotNull { (id, e) ->
        // Check that only restoring entities are saved (should be the norm).
        if (e is RestoringEntity)
            id to e::class.java.name
        else
            null
    }

    // Save that field.
    store.save("allEntities", constructors)

    // Save all restoring entities to the store.
    for ((i, e) in context.index)
        if (e is RestoringEntity)
            e.save(store.path(i.toString()))
}