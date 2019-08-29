package eu.metatools.mk.track

import eu.metatools.mk.util.labeledAs
import kotlin.properties.Delegates.observable
import kotlin.reflect.KMutableProperty

/**
 * A variable (without receiver) partaking in undo-tracking.
 * @param initialValue The initial value of the prop, also determines type in inference.
 */
fun <T> prop(initialValue: T) = observable(initialValue) { property, oldValue, _ ->
    if (property is KMutableProperty<*>)
        undos.get()?.let {
            it.add({ property.setter.call(oldValue) } labeledAs {
                "reset ${property.name} to $oldValue"
            })
        }
}

/**
 * A variable (with receiver, i.e., property of an object) partaking in undo-tracking.
 * @param initialValue The initial value of the prop, also determines type in inference.
 */
fun <T> Any.prop(initialValue: T) = observable(initialValue) { property, oldValue, _ ->
    if (property is KMutableProperty<*>)
        undos.get()?.let {
            it.add({ property.setter.call(this, oldValue) } labeledAs {
                "reset $this.${property.name} to $oldValue"
            })
        }
}