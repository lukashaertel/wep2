package eu.metatools.wep2.lang

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class DirectValue<T>(val value: T) : ReadOnlyProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>) = value
}