package eu.metatools.wep2.nes.dsl

import eu.metatools.wep2.nes.aspects.*
import eu.metatools.wep2.nes.dt.Lx
import eu.metatools.wep2.nes.dt.div
import eu.metatools.wep2.nes.dt.lx
import eu.metatools.wep2.nes.lang.ObservedSet
import eu.metatools.wep2.nes.lang.autoClosing
import eu.metatools.wep2.nes.structure.Container
import eu.metatools.wep2.nes.structure.Id
import eu.metatools.wep2.nes.structure.Part
import eu.metatools.wep2.util.delegates.ReadOnlyPropertyProvider
import java.util.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * A [Change] on a [SetProperty].
 * @property added The added elements.
 * @property removed The removed elements.
 */
data class SetChange<E>(
    val added: SortedSet<E>,
    val removed: SortedSet<E>
) : Change<SetChange<E>> {
    override fun merge(other: SetChange<E>) =
        SetChange(
            // A' union (A subtract R')
            TreeSet(other.added union (added subtract other.removed)),
            // R union (R' subtract A)
            TreeSet(removed union (other.removed subtract added))
        )

    override fun invert() =
        SetChange(removed, added)

    /**
     * Applies the change to a set.
     */
    fun apply(set: Set<E>) =
        // Compose via set arithmetic.
        (set - removed) + added

    /**
     * Applies the change to a mutable set.
     */
    fun applyTo(set: MutableSet<E>) {
        // Remove all removed elements.
        set.removeAll(removed)

        // Add all added elements.
        set.addAll(added)
    }
}

/**
 * A set property dealing with [Listen], [Track] and [Store] of [aspects]. Uses the full [id] and the given initial
 * value assignment [init] if needed.
 */
class SetProperty<E : Comparable<E>>(
    val aspects: Aspects?, override val id: Lx, val init: () -> List<E>
) : Id, Part, ReadOnlyProperty<Any?, NavigableSet<E>> {

    /**
     * The observed set dealing with notification of changes.
     */
    private lateinit var current: ObservedSet<E>

    /**
     * Close handle for store connection.
     */
    private var closeSave by autoClosing()

    private fun createObservedSet(from: List<E>) =
        ObservedSet(TreeSet(from)) { add, remove ->
            aspects<Listen> {
                changed(id, SetChange(add, remove))
            }

            aspects<Track> {
                resetWith(id) {
                    // Undo changes properly.
                    current.actual.removeAll(add)
                    current.actual.addAll(remove)

                    // If listening, notify changed back.
                    aspects<Listen> {
                        // Switch add and remove.
                        changed(id, SetChange(remove, add))
                    }
                }
            }
        }

    override fun connect() {
        // Load from store and register saving if needed.
        aspects<Store> {
            // Assign current value.
            current = if (isLoading)
            // If loading, retrieve from the store and deproxify.
                createObservedSet(aspects.toValue(load(id)) as List<E>)
            else
            // Not loading, just initialize.
                createObservedSet(init())

            // Register saving method.
            closeSave = save.register {
                // Save value with optional proxification.
                it(id, aspects.toProxy(current.toList()))
            }
        } ?: run {
            // Just initialize the value.
            val initValue = init()
            current = createObservedSet(initValue)
        }
    }

    override fun disconnect() {
        closeSave = null
    }

    override operator fun getValue(thisRef: Any?, property: KProperty<*>) =
        current.also {
            // Notify set was viewed.
            aspects<Listen> {
                viewed(id, it)
            }
        }

    override fun toString() =
        current.toString()
}

/**
 * Creates a [SetProperty] from the aspect scope. Determines the id from the [Id] aspect of the receiver combined
 * with the property name. If receiver has no [Id] aspect, the property name alone is used. Passes the given initial
 * value assignment [init]. If the receiver has a [Container] aspect, the created component will be housed in it.
 */
fun <E : Comparable<E>> set(init: () -> List<E> = ::emptyList) =
    ReadOnlyPropertyProvider { aspects: Aspects?, property ->
        // Compose ID if receiver provides ID, otherwise just use property.
        // Append to parent ID if present, otherwise start root.
        val id = aspects.with<Id>()?.let {
            it.id / property.name
        } ?: lx / property.name

        // Create set from implied values.
        SetProperty(aspects, id, init).also {
            // If receiver is a container, add it.
            aspects<Container> {
                include(id, it)
            }
        }
    }