package eu.metatools.wep2.track.bind

import eu.metatools.wep2.entity.Entity
import eu.metatools.wep2.entity.RestoringEntity
import eu.metatools.wep2.entity.bind.Restore
import eu.metatools.wep2.entity.bind.Store
import eu.metatools.wep2.lang.ReadWritePropertyProvider
import eu.metatools.wep2.track.undos
import eu.metatools.wep2.util.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1

/**
 * Stores an optional entity reference, provides restoration.
 */
fun <I, E : Entity<*, *, I>> ref(restore: Restore?, initial: () -> E? = { null }) =
    ReadWritePropertyProvider { thisRef: Entity<*, *, I>, property ->
        // Apply auto-saving mechanism.
        if (thisRef is RestoringEntity<*, *, *>) {
            // Type assert on result type, receiver is not null so this is a member property.
            @Suppress("unchecked_cast")
            property as KProperty1<Entity<*, *, I>, E?>

            thisRef.saveWith({ store: Store ->
                store.saveRef(property.name, property.get(thisRef))
            } labeledAs { "save ref ${property.name}" })
        }

        object : ReadWriteProperty<Entity<*, *, *>, E?> {
            /**
             * Stores the current value, initially not assigned.
             */
            var current: Option<E?> = None

            init {
                // Check if restoring.
                if (restore != null) {
                    // Is restoring, retrieve the property as the stored optional ID.
                    val id = restore.load<Option<I>>(property.name)

                    // See if ID is present.
                    if (id is Just) {
                        // ID is actually given, register resolution of the ID.
                        restore.registerPost {
                            current = Just(thisRef.context.index[id.item] as E?)
                        }
                    } else {
                        // ID is not present, original value was null.
                        current = Just(null)
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

            override fun setValue(thisRef: Entity<*, *, *>, property: KProperty<*>, value: E?) {
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
                    ?: "Ref, restore queued"
        }
    }

/**
 * Saves the value of the property with an optional entity reference to the receiver.
 */
fun <E : Entity<*, *, *>> Store.saveRef(name: String, value: E?) {
    save(name, value?.id?.let { Just(it) } ?: None)
}

/**
 * Saves the value of the property with an optional entity reference to the receiver.
 */
fun <E : Entity<*, *, *>> Store.saveRef(property: KProperty0<E?>) =
    saveRef(property.name, property.get())