package eu.metatools.wep2.track.bind

import eu.metatools.wep2.entity.RestoringEntity
import eu.metatools.wep2.entity.bind.Restore
import eu.metatools.wep2.entity.bind.Store
import eu.metatools.wep2.util.ReadWritePropertyProvider
import eu.metatools.wep2.track.prop
import eu.metatools.wep2.util.labeledAs
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1

/**
 * A [prop] that will be restored from the given map if possible, otherwise initialized with the
 * given initializer block. The name that this property restored from will be computed from the
 * target property name.
 */

fun <V> prop(restore: Restore?, block: () -> V) =
    // Create a property provider to receive the name.
    ReadWritePropertyProvider { thisRef: Any?, property ->
        // Apply auto-saving mechanism.
        if (thisRef is RestoringEntity<*, *, *>) {
            // Type assert on result type, receiver is not null so this is a member property.
            @Suppress("unchecked_cast")
            property as KProperty1<Any?, V>

            thisRef.saveWith({ store: Store ->
                store.saveProp(property.name, property.get(thisRef))
            } labeledAs { "save property ${property.name}" })
        }

        // Create a basic property, use the restored value if
        // in restore mode, otherwise initialize.
        if (restore != null)
            prop(restore.load(property.name))
        else
            prop(block())
    }

/**
 * Saves a simple property to the receiver.
 */
fun <V> Store.saveProp(name: String, value: V) {
    save(name, value)
}

/**
 * Saves a simple property to the receiver.
 */
fun <V> Store.saveProp(property: KProperty0<V>) {
    saveProp(property.name, property.get())
}