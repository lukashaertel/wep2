package eu.metatools.wep2.track.bind

import eu.metatools.wep2.entity.Entity
import eu.metatools.wep2.entity.RestoringEntity
import eu.metatools.wep2.entity.bind.Restore
import eu.metatools.wep2.entity.bind.Store
import eu.metatools.wep2.track.undos
import eu.metatools.wep2.util.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1

/**
 * Stores a mandatory entity reference, provides restoration.
 */
fun <E : Entity<*, *, *>> refOne(restore: Restore?, initial: () -> E) =
    ReadWritePropertyProvider { thisRef: Entity<*, *, *>, property ->
        // Apply auto-saving mechanism.
        if (thisRef is RestoringEntity<*, *, *>) {
            // Type assert on result type, receiver is not null so this is a member property.
            @Suppress("unchecked_cast")
            property as KProperty1<Entity<*, *, *>, E>

            thisRef.saveWith({ store: Store ->
                store.saveRefOne(property.name, property.get(thisRef))
            } labeledAs { "save mandatory ref ${property.name}" })
        }

        object : ReadWriteProperty<Entity<*, *, *>, E> {
            /**
             * Stores the current value, initially not assigned.
             */
            var current: Option<E> = None

            init {
                // Check if restoring.
                if (restore != null) {
                    // Is restoring, retrieve the property as the stored ID.
                    val id = restore.load<Comparable<Any?>>(property.name)

                    // Register resolution of the ID.
                    restore.registerPost {
                        // Cast index for access without type information.
                        @Suppress("unchecked_cast")
                        val index = thisRef.context.index as SimpleMap<Comparable<Any?>, E>

                        // Assign result, ID must be present in the result set.
                        current = Just(index[id] ?: throw IllegalStateException("Entity at $id not restored"))
                    }
                } else {
                    // Not restoring, offer initial value.
                    current = Just(initial())
                }
            }

            override fun getValue(thisRef: Entity<*, *, *>, property: KProperty<*>) =
                // Return the present value of current, otherwise throw an exception, something is going wrong.
                (current as? Just)?.item
                    ?: throw IllegalStateException("Trying to access field value while restoring")

            override fun setValue(thisRef: Entity<*, *, *>, property: KProperty<*>, value: E) {
                // Memorize old value before setting.
                val oldValue = current
                current = Just(value)

                // Check if there was an old value, this property is after all late initialized.
                if (oldValue is Just)
                    undos.get()?.let {
                        it.add({ current = oldValue } labeledAs {
                            "reset ref ${property.name} to $oldValue"
                        })
                    }
            }

            override fun toString() =
                (current as? Just)?.item?.toString()
                    ?: "Mandatory ref, restore queued"
        }
    }

/**
 * Saves the value of the property with a mandatory entity reference to the receiver.
 */
fun <E : Entity<*, *, *>> Store.saveRefOne(name: String, value: E) {
    save(name, value.id)
}

/**
 * Saves the value of the property with a mandatory entity reference to the receiver.
 */
fun <E : Entity<*, *, *>> Store.saveRefOne(property: KProperty0<E>) {
    saveRefOne(property.name, property.get())
}