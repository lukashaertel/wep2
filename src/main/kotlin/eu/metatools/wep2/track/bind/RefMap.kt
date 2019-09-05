package eu.metatools.wep2.track.bind

import eu.metatools.wep2.entity.Entity
import eu.metatools.wep2.entity.RestoringEntity
import eu.metatools.wep2.entity.bind.Restore
import eu.metatools.wep2.entity.bind.Store
import eu.metatools.wep2.lang.ReadOnlyPropertyProvider
import eu.metatools.wep2.util.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1

/**
 * Stores a map to entities, provides restoration.
 */
fun <I, K, E : Entity<*, *, I>> refMap(restore: Restore?) =
    ReadOnlyPropertyProvider { thisRef: Entity<*, *, I>, property ->
        // Apply auto-saving mechanism.
        if (thisRef is RestoringEntity<*, *, *>) {
            // Type assert on result type, receiver is not null so this is a member property.
            @Suppress("unchecked_cast")
            property as KProperty1<Entity<*, *, I>, SimpleMap<K, E>>

            thisRef.saveWith({ store: Store ->
                store.saveRefMap(property.name, property.get(thisRef))
            } labeledAs { "save ref map ${property.name}" })
        }

        object : ReadOnlyProperty<Entity<*, *, *>, SimpleMap<K, E>> {
            /**
             * Stores the current value, initially not assigned.
             */
            /**
             * Stores the current value, initially not assigned.
             */
            var current: Option<SimpleMap<K, E>> =
                None

            init {
                // Check if restoring.
                if (restore != null) {
                    // Is restoring, retrieve the property as the stored keys to IDs.
                    val ids = restore.load<List<Pair<K, I>>>(property.name)

                    // Register resolution of the IDs.
                    restore.registerPost {
                        current = Just(eu.metatools.wep2.track.map<K, E>().also { map ->
                            ids.forEach { (k, id) ->
                                map.silent[k] = thisRef.context.index[id] as E
                            }
                        })
                    }
                } else {
                    // Not restoring, offer new map.
                    current = Just(eu.metatools.wep2.track.map())
                }
            }

            override fun getValue(thisRef: Entity<*, *, *>, property: KProperty<*>) =
                // Return the present value of current, otherwise throw an exception, something is going wrong.
                (current as? Just)?.item
                    ?: throw IllegalStateException("Trying to access field value while restoring")

            override fun toString() =
                (current as? Just)?.item?.toString()
                    ?: "Ref map, restore queued"
        }
    }

/**
 * Saves the value of the property with a [SimpleMap] to entity references to the receiver.
 */
fun <K, E : Entity<*, *, *>> Store.saveRefMap(name: String, value: SimpleMap<K, E>) {
    save(name, value.map { (k, v) -> k to v.id })
}

/**
 * Saves the value of the property with a [SimpleMap] to entity references to the receiver.
 */
fun <K, E : Entity<*, *, *>> Store.saveRefMap(property: KProperty0<SimpleMap<K, E>>) {
    saveRefMap(property.name, property.get())
}