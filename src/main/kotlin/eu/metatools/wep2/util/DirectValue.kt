package eu.metatools.wep2.util

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Delegates to just returning [value].
 */
class DirectValue<T>(val value: T) : ReadOnlyProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>) = value

    override fun toString() =
        value.toString()
}