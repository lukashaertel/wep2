package eu.metatools.up.dsl

import eu.metatools.elt.Change
import eu.metatools.elt.Listen
import eu.metatools.up.Ent
import eu.metatools.up.Mode
import eu.metatools.up.Scope
import eu.metatools.up.dt.Lx
import eu.metatools.up.dt.div
import eu.metatools.up.lang.ObservedSet
import eu.metatools.up.lang.autoClosing
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

    override fun toString() =
        if (added.isEmpty()) {
            if (removed.isEmpty())
                "unchanged"
            else
                "-$removed"
        } else {

            if (removed.isEmpty())
                "+$added"
            else
                "+$added, -$removed"
        }
}

/**
 * A set property dealing with [Listen], [Track] and [Store] of [aspects]. Uses the full [id] and the given initial
 * value assignment [init] if needed.
 */
class SetProperty<E : Comparable<E>>(
    val scope: Scope,  val id: Lx, val init: () -> List<E>
) : Part, ReadOnlyProperty<Any?, NavigableSet<E>> {

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
            // If listening, notify changed.
            if (scope is Listen)
                scope.changed(id, SetChange(add, remove))
            scope.capture(id) {

                // Undo changes properly.
                current.actual.removeAll(add)
                current.actual.addAll(remove)

                // If listening, notify changed back.
                if (scope is Listen)
                    scope.changed(id, SetChange(remove, add))
            }
        }

    override fun connect() {
        // Assign current value.
        current = if (scope.mode == Mode.RestoreData)
        // If loading, retrieve from the store and deproxify.
            createObservedSet(scope.toValue(scope.load(id)) as List<E>)
        else
        // Not loading, just initialize.
            createObservedSet(init())

        // Register saving method.
        closeSave = scope.onSave.register {
            // Save value with optional proxification.
            scope.save(id, scope.toProxy(current.toList()))
        }
    }

    override fun disconnect() {
        closeSave = null
    }

    override operator fun getValue(thisRef: Any?, property: KProperty<*>) =
        current.also {
            // Notify set was viewed.
            if (scope is Listen)
                scope.viewed(id, it)
        }

    override fun toString() =
        current.toString()
}

fun <E : Comparable<E>> set(init: () -> List<E> = ::emptyList) =
    ReadOnlyPropertyProvider { ent: Ent, property ->
        // Append to containing entity.
        val id = ent.id / property.name

        // Create set from implied values.
        SetProperty(ent.scope, id, init).also {
            // Include in entity.
            ent.include(id, it)
        }
    }