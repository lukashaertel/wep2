package eu.metatools.wep2.entity

import eu.metatools.wep2.storage.Restore
import eu.metatools.wep2.storage.path
import kotlin.reflect.KFunction
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

/**
 * Restores to the [context] index all entities that can be retrieved in the [restore] object.
 *
 * A list of IDs and constructors should be located in the `"allEntities"` entry of the [restore] object.
 * @param context The context to restore to.
 * @param restore The restore object.
 */
fun <N, T : Comparable<T>, I : Comparable<I>> restoreIndex(
    context: Context<N, T, I>,
    restore: Restore
) {

    // Retrieve the list of all IDs associated to the constructors.
    val constructors = restore.load<List<Pair<I, String>>>("allEntities")

    // Store all already resolved constructors.
    val resolved = mutableMapOf<String, KFunction<RestoringEntity<*, *, I>>>()

    // Restore all entities individually.
    for ((i, c) in constructors) {
        val constructor = resolved.getOrPut(c) {
            @Suppress("unchecked_cast")
            Class.forName(c).kotlin.primaryConstructor as? KFunction<RestoringEntity<*, *, I>>?
                ?: throw IllegalArgumentException("No primary constructor for $c")
        }

        // Memorize if constructor was accessible.
        val wasAccessible = constructor.isAccessible

        try {
            // Make accessible.
            constructor.isAccessible = true

            // Call the constructor with a local restore object, only use argument positions here.
            val entity = constructor.callBy(
                mapOf(
                    constructor.parameters[0] to context,
                    constructor.parameters[1] to restore.path(i.toString())
                )
            )

            // Assign the ID of the entity.
            entity.id = i
        } finally {
            // Reset accessibility.
            constructor.isAccessible = wasAccessible
        }
    }
}

