package eu.metatools.wep2.track.bind

import eu.metatools.wep2.entity.RestoringEntity
import eu.metatools.wep2.entity.bind.Restore
import eu.metatools.wep2.entity.bind.Store
import eu.metatools.wep2.lang.DirectValue
import eu.metatools.wep2.lang.ReadOnlyPropertyProvider
import eu.metatools.wep2.track.set
import eu.metatools.wep2.track.map
import eu.metatools.wep2.util.SimpleMap
import eu.metatools.wep2.util.SimpleSet
import eu.metatools.wep2.util.labeledAs
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1

/**
 * A set partaking in change based undo-tracking. Alternatively, prop of an immutable set can be used.
 */
fun <E> set(restore: Restore?) =
    // Create a property provider to receive the name.
    ReadOnlyPropertyProvider { thisRef: Any?, property ->
        // Apply auto-saving mechanism.
        if (thisRef is RestoringEntity<*, *, *>) {
            // Type assert on result type, receiver is not null so this is a member property.
            @Suppress("unchecked_cast")
            property as KProperty1<Any?, SimpleSet<E>>

            thisRef.saveWith({ store: Store ->
                store.saveSet(property.name, property.get(thisRef))
            } labeledAs { "Save set ${property.name}" })
        }

        // Check if currently restoring.
        if (restore != null) {
            // Restoring, load values.
            val values = restore.load<List<E>>(property.name)

            // Return set with values.
            DirectValue(set<E>().also { set ->
                values.forEach {
                    set.silent.add(it)
                }
            })
        } else {
            // Otherwise, just create new set.
            DirectValue(set())
        }
    }

/**
 * Saves the value of the property with a [SimpleSet] to the receiver.
 */
fun <E> Store.saveSet(name: String, value: SimpleSet<E>) {
    save(name, value.toList())
}

/**
 * Saves the value of the property with a [SimpleSet] to the receiver.
 */
fun <E> Store.saveSet(property: KProperty0<SimpleSet<E>>) =
    saveSet(property.name, property.get())

/**
 * A map partaking in change based undo-tracking. Alternatively, prop of an immutable map can be used.
 */
fun <K, V> map(restore: Restore?) =
// Create a property provider to receive the name.
    ReadOnlyPropertyProvider { thisRef: Any?, property ->
        // Apply auto-saving mechanism.
        if (thisRef is RestoringEntity<*, *, *>) {
            // Type assert on result type, receiver is not null so this is a member property.
            @Suppress("unchecked_cast")
            property as KProperty1<Any?, SimpleMap<K, V>>

            thisRef.saveWith({ store: Store ->
                store.saveMap(property.name, property.get(thisRef))
            } labeledAs { "save map ${property.name}" })
        }

        // Check if currently restoring.
        if (restore != null) {
            // Restoring, load entries.
            val values = restore.load<List<Pair<K, V>>>(property.name)

            // Return map with entries.
            DirectValue(map<K, V>().also { map ->
                values.forEach { (k, v) ->
                    map.silent[k] = v
                }
            })
        } else {
            // Otherwise, just create new map.
            DirectValue(map())
        }
    }

/**
 * Saves the value of the property with a [SimpleMap] to the receiver.
 */
fun <K, V> Store.saveMap(name: String, value: SimpleMap<K, V>) {
    save(name, value.map { (k, v) -> k to v })
}

/**
 * Saves the value of the property with a [SimpleMap] to the receiver.
 */
fun <K, V> Store.saveMap(property: KProperty0<SimpleMap<K, V>>) =
    saveMap(property.name, property.get())