package eu.metatools.wep2.track

import eu.metatools.wep2.util.labeledAs
import kotlin.properties.Delegates.observable
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty

/**
 * A variable partaking in undo-tracking.
 * @param initialValue The initial value of the prop, also determines type in inference.
 */
fun <T> prop(initialValue: T) = object : ReadWriteProperty<Any?, T> {
    private var current = initialValue

    override fun getValue(thisRef: Any?, property: KProperty<*>) =
        current

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        if (current == value)
            return

        val oldValue = current
        current = value

        when (property) {
            // If mutable property without receiver, set value.
            is KMutableProperty0<*> -> undos.get()?.let {
                it.add({ property.setter.call(oldValue) } labeledAs {
                    "reset ${property.name} to $oldValue"
                })
            }

            // If mutable property with receiver, set value on this reference.
            is KMutableProperty1<*, *> -> undos.get()?.let {
                it.add({ property.setter.call(thisRef, oldValue) } labeledAs {
                    "reset $thisRef.${property.name} to $oldValue"
                })
            }
        }
    }
}
