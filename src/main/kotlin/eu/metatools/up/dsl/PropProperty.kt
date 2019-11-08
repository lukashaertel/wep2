package eu.metatools.wep2.nes.dsl

import eu.metatools.wep2.nes.aspects.*
import eu.metatools.wep2.nes.dt.Box
import eu.metatools.wep2.nes.dt.Lx
import eu.metatools.wep2.nes.dt.div
import eu.metatools.wep2.nes.dt.lx
import eu.metatools.wep2.nes.lang.autoClosing
import eu.metatools.wep2.nes.structure.Container
import eu.metatools.wep2.nes.structure.Id
import eu.metatools.wep2.nes.structure.Part
import eu.metatools.wep2.util.delegates.ReadWritePropertyProvider
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * A [Change] on a [PropProperty].
 * @property from The original value.
 * @property to The new value.
 */
data class PropChange<T>(val from: T, val to: T) : Change<PropChange<T>> {
    override fun merge(other: PropChange<T>) =
        PropChange(from, other.to)

    override fun invert() =
        PropChange(to, from)
}

/**
 * A prop property dealing with [Listen], [Track] and [Store] of [aspects]. Uses the full [id] and the given initial
 * value assignment [init] if needed.
 */
class PropProperty<T>(
    val aspects: Aspects?, override val id: Lx, val init: () -> T
) : Id, Part, ReadWriteProperty<Any?, T> {

    /**
     * The actual value.
     */
    private lateinit var current: Box<T>

    /**
     * Close handle for store connection.
     */
    private var closeSave by autoClosing()

    override fun connect() {
        // Load from store and register saving if needed.
        aspects<Store> {
            // Assign current value.
            current = if (isLoading)
            // If loading, retrieve from the store and deproxify.
                Box(aspects.toValue(load(id)) as T)
            else
            // Not loading, just initialize.
                Box(init())

            // Register saving method.
            closeSave = save.register {
                // Save value content with optional proxification.
                it(id, aspects.toProxy(current.value))
            }
        } ?: run {
            // Just initialize the value.
            current = Box(init())
        }
    }

    override fun disconnect() {
        closeSave = null
    }

    override operator fun getValue(thisRef: Any?, property: KProperty<*>) =
        current.value.also {
            // Notify map was viewed.
            aspects<Listen> {
                viewed(id, it)
            }
        }

    override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        // Change value.
        val previous = current.value
        current = Box(value)

        // If listening, notify changed.
        aspects<Listen> {
            changed(id, PropChange(previous, value))
        }

        // If tracking, provide undo.
        aspects<Track> {
            resetWith(id) {
                current = Box(previous)

                // If listening, notify changed back.
                aspects<Listen> {
                    changed(id, PropChange(value, previous))
                }
            }
        }
    }

    override fun toString() =
        current.value.toString()
}

/**
 * Creates a [PropProperty] with the name of the receiving entity plus the property name. Passes the receivers aspects. Also
 * adds the result to the components of the entity.
 */
fun <T> prop(init: () -> T) =
    ReadWritePropertyProvider { aspects: Aspects?, property ->
        // Append to parent ID if present, otherwise start root.
        val id = aspects.with<Id>()?.let {
            it.id / property.name
        } ?: lx / property.name

        // Create property from implied values.
        PropProperty(aspects, id, init).also {
            // If receiver is a container, add it.
            aspects<Container> {
                include(id, it)
            }
        }
    }