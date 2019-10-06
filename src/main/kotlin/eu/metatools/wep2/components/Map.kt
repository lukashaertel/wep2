package eu.metatools.wep2.components

import eu.metatools.wep2.entity.Entity
import eu.metatools.wep2.entity.RestoringEntity
import eu.metatools.wep2.storage.Store
import eu.metatools.wep2.track.undos
import eu.metatools.wep2.util.ReadOnlyPropertyProvider
import eu.metatools.wep2.util.collections.ObservableMap
import eu.metatools.wep2.util.collections.ObservableMapListener
import eu.metatools.wep2.util.collections.SimpleMap
import eu.metatools.wep2.util.labeledAs
import eu.metatools.wep2.util.listeners.MapListener
import eu.metatools.wep2.util.listeners.mapListener
import eu.metatools.wep2.util.listeners.plus
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.isSubclassOf

/**
 * Delegates as a map, judging if proxy storage should be used by reflection.
 */
inline fun <K : Comparable<K>, reified V> map(listener: ObservableMapListener<K, V> = MapListener.EMPTY) =
    map(listener, V::class.isSubclassOf(Entity::class))

/**
 * Delegates as a map, notifies [listener] on changes, initially empty if not restored. If [entity] is
 * true, the values will be saved via ID proxy.
 */
fun <K : Comparable<K>, V> map(listener: ObservableMapListener<K, V> = MapListener.EMPTY, entity: Boolean) =
    @Suppress("unchecked_cast")
    ReadOnlyPropertyProvider { thisRef: Any?, property ->
        // Get reference and see if it is a restoring entity.
        val asEntity = thisRef as? Entity<*, *, *>
        val asRestoring = thisRef as? RestoringEntity<*, *, *>

        // Get the index if this is an entity.
        val index = asEntity?.context?.index as? SimpleMap<Comparable<Any?>, V>

        // Return a property that notifies, tracks and restores if needed.
        object : ReadOnlyProperty<Any?, ObservableMap<K, V>> {
            /**
             * The actual set
             */
            val implementation = ObservableMap(
                mapListener<ObservableMap<K, V>, K, V>(
                    { k, _ ->
                        // Run on undos if assigned.
                        undos.get()?.let {
                            // Add removing the just added entry.
                            it.add({ silent.remove(k); Unit } labeledAs {
                                "remove $k on $this"
                            })
                        }
                    },
                    { k, v, _ ->
                        undos.get()?.let {
                            // Add resetting entry to previous value.
                            it.add({ silent[k] = v } labeledAs {
                                "set $k=$v on $this"
                            })
                        }
                    },
                    { k, v ->
                        undos.get()?.let {
                            // Add re-adding the just removed entry.
                            it.add({ silent[k] = v } labeledAs {
                                "add $k=$v on $this"
                            })
                        }
                    }) + listener
            )

            init {
                // Load as proxies if entity, otherwise
                loadProxified(
                    asRestoring?.restore, property.name, entity,
                    { emptyList<Pair<K, V>>() },
                    { proxy: List<Pair<Comparable<Any?>, Comparable<Any?>>> ->
                        proxy.map { (k, v) ->
                            k to index?.get(v)
                        }
                    },
                    { items ->
                        items?.forEach { (k, v) ->
                            implementation[k as K] = v as V
                        }
                    }
                )
            }

            override fun getValue(thisRef: Any?, property: KProperty<*>) =
                implementation

            init {
                // Amend save if in restoring entity.
                asRestoring?.saveWith({ store: Store ->
                    // Based on the kind of property, save proxies or values.
                    storeProxified(store, property.name, entity, implementation.map { it.toPair() }, {
                        it.map { (k, v) -> k to (v as Entity<*, *, *>).id }
                    })
                } labeledAs {
                    "save map ${property.name}"
                })
            }

            override fun toString() =
                implementation.toString()
        }
    }