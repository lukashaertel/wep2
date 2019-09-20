package eu.metatools.wep2.track.bind

import eu.metatools.wep2.entity.Entity
import eu.metatools.wep2.entity.RestoringEntity
import eu.metatools.wep2.entity.bind.Restore
import eu.metatools.wep2.entity.bind.Store
import eu.metatools.wep2.track.set
import eu.metatools.wep2.util.ReadOnlyPropertyProvider
import eu.metatools.wep2.util.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1

/**
 * Stores a set of entities, provides restoration.
 */
fun <E> refSet(restore: Restore?) where  E : Entity<*, *, *>, E : Comparable<E> =
    ReadOnlyPropertyProvider { thisRef: Entity<*, *, *>, property ->
        // Apply auto-saving mechanism.
        if (thisRef is RestoringEntity<*, *, *>) {
            // Type assert on result type, receiver is not null so this is a member property.
            @Suppress("unchecked_cast")
            property as KProperty1<Entity<*, *, *>, SimpleSet<E>>

            thisRef.saveWith({ store: Store ->
                store.saveRefSet(property.name, property.get(thisRef))
            } labeledAs { "save ref set ${property.name}" })
        }

        object : ReadOnlyProperty<Entity<*, *, *>, SimpleSet<E>> {
            /**
             * Stores the current value, initially not assigned.
             */
            var current: Option<SimpleSet<E>> =
                None

            init {
                // Check if restoring.
                if (restore != null) {
                    // Is restoring, retrieve the property as the stored IDs.
                    val ids = restore.load<List<Comparable<Any?>>>(property.name)

                    // Register resolution of the IDs.
                    restore.registerPost {
                        // Cast index for access without type information.
                        @Suppress("unchecked_cast")
                        val index = thisRef.context.index as SimpleMap<Comparable<Any?>, E>

                        current = Just(set<E>().also { set ->
                            ids.forEach {
                                set.silent.add(index[it] ?: throw IllegalStateException("Entity at $it not restored"))
                            }
                        })
                    }
                } else {
                    // Not restoring, offer new set.
                    current = Just(set())
                }
            }

            override fun getValue(thisRef: Entity<*, *, *>, property: KProperty<*>) =
                // Return the present value of current, otherwise throw an exception, something is going wrong.
                (current as? Just)?.item
                    ?: throw IllegalStateException("Trying to access field value while restoring")

            override fun toString() =
                (current as? Just)?.item?.toString()
                    ?: "Ref set, restore queued"
        }
    }

/**
 * Saves the value of the property with a [SimpleSet] of entity references to the receiver.
 */
fun <E> Store.saveRefSet(name: String, value: SimpleSet<E>) where  E : Entity<*, *, *>, E : Comparable<E> {
    save(name, value.map { it.id })
}

/**
 * Saves the value of the property with a [SimpleSet] of entity references to the receiver.
 */
fun <E> Store.saveRefSet(property: KProperty0<SimpleSet<E>>) where  E : Entity<*, *, *>, E : Comparable<E> {
    saveRefSet(property.name, property.get())
}