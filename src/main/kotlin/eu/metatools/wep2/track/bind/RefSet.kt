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
 * Stores a set of entities, provides restoration.
 */
fun <I, E : Entity<*, *, I>> refSet(restore: Restore?) =
    ReadOnlyPropertyProvider { thisRef: Entity<*, *, I>, property ->
        // Apply auto-saving mechanism.
        if (thisRef is RestoringEntity<*, *, *>) {
            // Type assert on result type, receiver is not null so this is a member property.
            @Suppress("unchecked_cast")
            property as KProperty1<Entity<*, *, I>, SimpleSet<E>>

            thisRef.saveWith({ store: Store ->
                store.saveRefSet(property.name, property.get(thisRef))
            } labeledAs { "save ref set ${property.name}" })
        }

        object : ReadOnlyProperty<Entity<*, *, *>, SimpleSet<E>> {
            /**
             * Stores the current value, initially not assigned.
             */
            /**
             * Stores the current value, initially not assigned.
             */
            var current: Option<SimpleSet<E>> =
                None

            init {
                // Check if restoring.
                if (restore != null) {
                    // Is restoring, retrieve the property as the stored IDs.
                    val ids = restore.load<List<I>>(property.name)

                    // Register resolution of the IDs.
                    restore.registerPost {
                        current = Just(eu.metatools.wep2.track.set<E>().also { set ->
                            ids.forEach {
                                set.silent.add(thisRef.context.index[it] as E)
                            }
                        })
                    }
                } else {
                    // Not restoring, offer new set.
                    current = Just(eu.metatools.wep2.track.set())
                }
            }

            override fun getValue(thisRef: Entity<*, *, *>, property: KProperty<*>) =
                // Return the present value of current, otherwise throw an exception, something is going wrong.
                (current as? Just)?.item
                    ?: throw IllegalStateException("Trying to access field value while restoring")
        }
    }

/**
 * Saves the value of the property with a [SimpleSet] of entity references to the receiver.
 */
fun <E : Entity<*, *, *>> Store.saveRefSet(name: String, value: SimpleSet<E>) {
    save(name, value.map { it.id })
}

/**
 * Saves the value of the property with a [SimpleSet] of entity references to the receiver.
 */
fun <E : Entity<*, *, *>> Store.saveRefSet(property: KProperty0<SimpleSet<E>>) {
    saveRefSet(property.name, property.get())
}