package eu.metatools.wep2.components

import eu.metatools.wep2.entity.Entity
import eu.metatools.wep2.aspects.Resolving
import eu.metatools.wep2.aspects.Restoring
import eu.metatools.wep2.aspects.Saving
import eu.metatools.wep2.storage.Store
import eu.metatools.wep2.storage.loadProxified
import eu.metatools.wep2.storage.storeProxified
import eu.metatools.wep2.track.undos
import eu.metatools.wep2.util.delegates.ReadOnlyPropertyProvider
import eu.metatools.wep2.util.collections.ObservableSet
import eu.metatools.wep2.util.collections.ObservableSetListener
import eu.metatools.wep2.util.labeledAs
import eu.metatools.wep2.util.listeners.SetListener
import eu.metatools.wep2.util.listeners.plus
import eu.metatools.wep2.util.listeners.setListener
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.isSubclassOf

/**
 * Delegates as a set, judging if proxy storage should be used by reflection.
 */
inline fun <reified E : Comparable<E>> set(listener: ObservableSetListener<E> = SetListener.EMPTY) =
    set(listener, E::class.isSubclassOf(Entity::class))

/**
 * Delegates as a set, notifies [listener] on changes, initially empty if not restored. If [entity] is
 * true, the values will be saved via ID proxy.
 */
fun <E : Comparable<E>> set(listener: ObservableSetListener<E> = SetListener.EMPTY, entity: Boolean) =
    @Suppress("unchecked_cast")
    (ReadOnlyPropertyProvider { thisRef: Any?, property ->
        // Get aspects of the receiver.
        val restoring = thisRef as? Restoring
        val saving = thisRef as? Saving
        val resolving = thisRef as? Resolving<Comparable<Any?>, E>

        // Return a property that notifies, tracks and restores if needed.
        object : ReadOnlyProperty<Any?, ObservableSet<E>> {
            /**
             * The actual set
             */
            /**
             * The actual set
             */
            val implementation = ObservableSet(
                setListener<ObservableSet<E>, E>(
                    { e ->
                        // Run on undos if assigned.
                        undos.get()?.let {
                            // Add removing the just added element.
                            it.add({ silent.remove(e); Unit } labeledAs {
                                "remove $e on ${property.name}"
                            })
                        }
                    },
                    { e ->
                        undos.get()?.let {
                            // Add re-adding the just removed element.
                            it.add({ silent.add(e); Unit } labeledAs {
                                "add $e on ${property.name}"
                            })
                        }
                    }) + listener
            )

            init {
                // Load as proxies if entity, otherwise
                loadProxified(
                    restoring?.restore, property.name, entity,
                    { emptyList<E>() },
                    { proxy: List<Comparable<Any?>> ->
                        check(resolving != null) {
                            "Restoring entity proxies, ${property.name} must be from a resolving receiver."
                        }

                        proxy.map { resolving.resolve(it) }
                    },
                    { items ->
                        items?.forEach {
                            implementation.add(it as E)
                        }
                    }
                )
            }

            override fun getValue(thisRef: Any?, property: KProperty<*>) =
                implementation

            init {
                // Amend save if in appropriate aspect.
                saving?.saveWith({ store: Store ->
                    // Based on the kind of property, save proxies or values.
                    storeProxified(store, property.name, entity, implementation.toList(), {
                        it.map { item -> (item as Entity<*, *, *>).id }
                    })
                } labeledAs {
                    "save set ${property.name}"
                })
            }

            override fun toString() =
                implementation.toString()
        }
    })