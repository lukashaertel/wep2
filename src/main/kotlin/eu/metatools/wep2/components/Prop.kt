package eu.metatools.wep2.components

import eu.metatools.wep2.entity.Entity
import eu.metatools.wep2.entity.RestoringEntity
import eu.metatools.wep2.storage.Store
import eu.metatools.wep2.track.undos
import eu.metatools.wep2.util.ReadWritePropertyProvider
import eu.metatools.wep2.util.collections.SimpleMap
import eu.metatools.wep2.util.labeledAs
import eu.metatools.wep2.util.listeners.Listener
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.isSubclassOf

/**
 * Delegates as a variable, judging if proxy storage should be used by reflection.
 */
inline fun <reified V> prop(listener: Listener<Unit, V> = Listener.EMPTY, noinline initial: () -> V) =
    prop(listener, initial, V::class.isSubclassOf(Entity::class))

/**
 * Delegates as a variable, notifies [listener] on changes, uses [initial] value if not restored. If [entity] is
 * true, the value will be saved via ID proxy.
 */
fun <V> prop(listener: Listener<Unit, V> = Listener.EMPTY, initial: () -> V, entity: Boolean) =
    @Suppress("unchecked_cast")
    ReadWritePropertyProvider { thisRef: Any?, property ->
        // Get reference and see if it is a restoring entity.
        val asEntity = thisRef as? Entity<*, *, *>
        val asRestoring = thisRef as? RestoringEntity<*, *, *>

        // Get the index if this is an entity.
        val index = asEntity?.context?.index as? SimpleMap<Comparable<Any?>, V>

        // Return a property that notifies, tracks and restores if needed.
        object : ReadWriteProperty<Any?, V> {
            /**
             *
             */
            var current by loadProxified(
                asRestoring?.restore, property.name, entity, initial,
                { proxy: Comparable<Any?> -> index?.get(proxy) },
                { listener.initialized(Unit, it as V) })

            override fun getValue(thisRef: Any?, property: KProperty<*>) =
                current as V

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: V) {
                // Don't act if no change of value.
                if (current == value)
                    return

                // Memorize previous value, change to new value, notify listener.
                val previous = current
                current = value
                listener.changed(Unit, previous as V, current as V)

                // Track the undo if open.
                undos.get()?.add({
                    // Memorize current value, set to old value, notify listener.
                    val prePrevious = current
                    current = previous
                    listener.changed(Unit, prePrevious as V, current as V)
                } labeledAs {
                    // Label as resetting.
                    "reset ${property.name} to $previous"
                })
            }

            init {
                // Amend save if in restoring entity.
                asRestoring?.saveWith({ store: Store ->
                    // Based on the kind of property, save proxy or value.
                    storeProxified(store, property.name, entity, current, {
                        (it as Entity<*, *, *>?)?.id
                    })
                } labeledAs {
                    "save property ${property.name}"
                })
            }

            override fun toString() =
                current.toString()
        }
    }