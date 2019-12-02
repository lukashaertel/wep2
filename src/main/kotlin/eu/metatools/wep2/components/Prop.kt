package eu.metatools.wep2.components

import eu.metatools.wep2.entity.Entity
import eu.metatools.wep2.aspects.Resolving
import eu.metatools.wep2.aspects.Restoring
import eu.metatools.wep2.aspects.Saving
import eu.metatools.wep2.storage.Store
import eu.metatools.wep2.storage.loadProxified
import eu.metatools.wep2.storage.storeProxified
import eu.metatools.wep2.track.undos
import eu.metatools.wep2.util.delegates.ReadWritePropertyProvider
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
    (ReadWritePropertyProvider { thisRef: Any?, property ->
        // Get aspects of the receiver.
        val restoring = thisRef as? Restoring
        val saving = thisRef as? Saving
        val resolving = thisRef as? Resolving<Comparable<Any?>, V>

        // Return a property that notifies, tracks and restores if needed.
        object : ReadWriteProperty<Any?, V> {
            var current by loadProxified(
                restoring?.restore, property.name, entity, initial,
                { proxy: Comparable<Any?> ->
                    check(resolving != null) {
                        "Restoring entity proxies, ${property.name} must be from a resolving receiver."
                    }

                    resolving.resolve(proxy)
                },
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
                // Amend save if in appropriate aspect.
                saving?.saveWith({ store: Store ->
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
    })