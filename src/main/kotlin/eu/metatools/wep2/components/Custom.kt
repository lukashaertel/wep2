package eu.metatools.wep2.components

import eu.metatools.wep2.aspects.Restoring
import eu.metatools.wep2.aspects.Saving
import eu.metatools.wep2.storage.Restore
import eu.metatools.wep2.storage.Store
import eu.metatools.wep2.util.delegates.ReadOnlyPropertyProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Custom value/restore/save hook.
 */
fun <V> custom(evaluate: () -> V, restore: (Restore, String) -> Unit, save: (Store, String, V) -> Unit) =
    ReadOnlyPropertyProvider { thisRef: Any?, property ->
        // Get aspects of the receiver.
        val restoring = thisRef as? Restoring
        val saving = thisRef as? Saving

        restoring?.restore?.let {
            restore(it, property.name)
        }

        saving?.saveWith {
            save(it, property.name, evaluate())
        }

        object : ReadOnlyProperty<Any?, V> {
            override fun getValue(thisRef: Any?, property: KProperty<*>) = evaluate()
        }
    }

/**
 * Custom restore/save hook without value association.
 */
fun custom(restore: (Restore, String) -> Unit, save: (Store, String) -> Unit) =
    custom({ Unit }, restore, { store, key, _ -> save(store, key) })