package eu.metatools.wep2.util

import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Delegates to just returning [value].
 */
class Property<T>(val value: T) : ReadOnlyProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>) = value

    override fun toString() =
        value.toString()
}

/**
 * A mutable variable with the given initial value.
 */
class MutableProperty<T>(initial: T) : ReadWriteProperty<Any?, T> {
    var current = initial

    override fun getValue(thisRef: Any?, property: KProperty<*>) = current

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        current = value
    }

    override fun toString() =
        current.toString()
}