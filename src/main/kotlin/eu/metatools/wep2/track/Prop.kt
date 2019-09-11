package eu.metatools.wep2.track

import eu.metatools.wep2.util.labeledAs
import kotlin.properties.Delegates.observable
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.isAccessible

/**
 * A variable partaking in undo-tracking.
 * @param initialValue The initial value of the prop, also determines type in inference.
 */
fun <T> prop(initialValue: T) = object : ReadWriteProperty<Any?, T> {
    private var current = initialValue

    override fun getValue(thisRef: Any?, property: KProperty<*>) =
        current

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        // Don't act if no change of value.
        if (current == value)
            return

        // Store old value, assign new value.
        val oldValue = current
        current = value

        undos.get()?.add({
            // Set current value to old value.
            current = oldValue
        } labeledAs {
            // Label as resetting.
            "reset ${property.name} to $oldValue"
        })
    }

    override fun toString() = current.toString()
}
