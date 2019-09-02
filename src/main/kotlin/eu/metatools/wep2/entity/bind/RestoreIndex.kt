package eu.metatools.wep2.entity.bind

import eu.metatools.wep2.entity.Context
import eu.metatools.wep2.entity.RestoringEntity
import kotlin.reflect.KFunction
import kotlin.reflect.full.primaryConstructor

/**
 * Restores to the [context] index all entities that can be retrieved in the [restore] object.
 *
 * A list of IDs and constructors should be located in the `"allEntities"` entry of the [restore] object.
 * @param context The context to restore to.
 * @param restore The restore object.
 */
fun <N, T : Comparable<T>, I> restoreIndex(
    context: Context<N, T, I>,
    restore: Restore
) {
    // Retrieve the list of all IDs associated to the constructors.
    val allEntities = restore.load<List<Pair<I, KFunction<RestoringEntity<*, *, I>>>>>("allEntities")

    // Restore all entities individually.
    for ((i, c) in allEntities) {
        // Call the constructor with a local restore object.
        val entity = c.call(context, restore.path(i.toString()))

        // Assign the ID of the entity.
        entity.id = i
    }
}

/**
 * Stores all entities in the index of the [context] to the [store] object. The store will be filled with an
 * `"allEntities"` entry memorizing the IDs and the classes of the entities via their primary constructors.
 *
 * @param context The context to store from.
 * @param store The store object.
 */
fun <N, T : Comparable<T>, I> storeIndex(
    context: Context<N, T, I>,
    store: Store
) {
    // Create the allEntities field from IDs and primary constructors.
    val allEntities = context.index.mapNotNull { (id, e) ->
        // Check that only restoring entities are saved (should be the norm).
        if (e is RestoringEntity)
            id to e::class.primaryConstructor
        else
            null
    }

    // Save that field.
    store.save("allEntities", allEntities)

    // Save all restoring entities to the store.
    for ((i, e) in context.index)
        if (e is RestoringEntity)
            e.save(store.path(i.toString()))
}