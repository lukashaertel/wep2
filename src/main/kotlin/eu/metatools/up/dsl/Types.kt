package eu.metatools.up.dsl

import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.full.isSupertypeOf
import kotlin.reflect.typeOf

/**
 * An exception from [Types.performTypeCheck].
 * @property on The receiver class.
 * @property property The property under check.
 * @property description The description of the type of problem.
 */
class TypeException(
    val on: KClass<*>,
    val property: KProperty<*>,
    val description: String
) : Exception(describe(on, property, description)) {
    companion object {
        fun describe(on: KClass<*>, property: KProperty<*>, description: String) =
            "Type warning for ${property.returnType} in ${on.simpleName}.${property.name}: $description"
    }
}

object Types {
    @Suppress("experimental_api_usage_error")
    private val mutableCollectionType = typeOf<MutableCollection<*>>()

    @Suppress("experimental_api_usage_error")
    private val hashSetType = typeOf<HashSet<*>>()

    @Suppress("experimental_api_usage_error")
    private val hashMapType = typeOf<HashMap<*, *>>()

    /**
     * Set of checked properties.
     */
    private val checked = mutableSetOf<KProperty<*>>()

    /**
     * If true, only warnings are generated.
     */
    var warnOnly = false

    /**
     * Performs type checking for the given [property] of the [receiver].
     * @param receiver The receiver instance.
     * @param property The property to check.
     * @param rootValidMutable True if the first level type may be a mutable collection.
     */
    fun performTypeCheck(receiver: Any, property: KProperty<*>, rootValidMutable: Boolean) {
        // Already checked, return.
        if (!checked.add(property))
            return

        // Displays a warning for the receiver and property.
        fun report(description: String) {
            if (warnOnly)
                println(TypeException.describe(receiver::class, property, description))
            else
                throw TypeException(receiver::class, property, description)
        }

        // Performs a recursive type check.
        fun check(type: KType, allowMutable: Boolean) {
            // Warn for mutable collections.
            if (!allowMutable && mutableCollectionType.isSupertypeOf(type))
                report("Use immutable type or use set or map property. ")

            // Warn for hash based types.
            if (hashSetType.isSupertypeOf(type))
                report("Use stable set or set property.")

            if (hashMapType.isSupertypeOf(type))
                report("Use stable map or map property.")

            // Warn for each argument.
            type.arguments.forEach {
                it.type?.let {
                    check(it, false)
                }
            }
        }

        // Check the return type of the property.
        check(property.returnType, rootValidMutable)
    }
}