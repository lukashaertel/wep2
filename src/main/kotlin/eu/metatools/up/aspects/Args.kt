package eu.metatools.up.aspects

import kotlin.reflect.KProperty0

/**
 * Provides notation of extra arguments.
 */
interface Args : Aspect {
    /**
     * The extra constructor arguments.
     */
    val extraArgs: Map<String, Any?>
}

/**
 * Creates the map of the property names to their current values.
 */
fun fromValuesOf(vararg entries: KProperty0<Any?>) =
    entries.associate { it.name to it.get() }