package eu.metatools.up.lang

import eu.metatools.up.dt.Box
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.isSupertypeOf
import kotlin.reflect.full.starProjectedType

/**
 * Returns true if the value is an instance of the type. This is a loose check, as the value type is star projected.
 */
operator fun KType.contains(value: Any?) =
    if (value == null) isMarkedNullable else (classifier as? KClass<*>)?.isInstance(value) ?: false

/**
 * Constructs the class by a map of parameter names to values.
 */
fun <T : Any> KClass<T>.constructBy(args: Map<String, Any?>, overflow: (KParameter) -> Box<Any?>? = { null }): T {
    // Try all present constructors.
    next@ for (constructor in constructors) {
        // Associate parameters by their name and create the actual assignment map.
        val associated = constructor.parameters.associateByTo(hashMapOf()) { it.name }
        val actual = mutableMapOf<KParameter, Any?>()

        // Transform all arguments.
        for ((name, value) in args) {
            // Get the actual parameter, skip constructor if parameter not present.
            val target = associated.remove(name) ?: continue@next

            // Match value against type or skip constructor.
            if (value in target.type)
                actual[target] = value
            else
                continue@next
        }

        // Associate remaining parameters.
        for (param in associated.values) {
            // Get from the overflow mapper.
            val extra = overflow(param)

            // If overflow mapper had value, use it, otherwise skip constructor.
            if (extra != null)
                actual[param] = extra.value
            else
                continue@next
        }

        // Call the constructor.
        return constructor.callBy(actual)
    }

    // Mark error.
    throw NoSuchElementException("No constructor in $simpleName for $args")
}