package eu.metatools.up.dsl

import eu.metatools.elt.Change
import eu.metatools.elt.Listen
import eu.metatools.up.*
import eu.metatools.up.dt.Lx
import eu.metatools.up.dt.div
import eu.metatools.up.lang.ObservedSet
import eu.metatools.up.lang.autoClosing
import eu.metatools.up.Mode
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
 * Set property, uses the full [id] and the given initial value assignment [init] on non-restore.
 */
class SetProperty<E : Comparable<E>>(
    val shell: Shell, val id: Lx, val init: () -> List<E>
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
            (shell.engine as? Listen)
                ?.changed(id, SetChange(add, remove))
            shell.engine.capture(id) {

                // Undo changes properly.
                current.actual.removeAll(add)
                current.actual.addAll(remove)

                // If listening, notify changed back.
                (shell.engine as? Listen)
                    ?.changed(id, SetChange(remove, add))
            }
        }

    override fun connect() {
        // Assign current value.
        current = if (shell.engine.mode == Mode.RestoreData)
        // If loading, retrieve from the store and deproxify.
            @Suppress("unchecked_cast")
            createObservedSet(shell.engine.toValue(shell.engine.load(id)) as List<E>)
        else
        // Not loading, just initialize.
            createObservedSet(init())

        // Register saving method.
        closeSave = shell.engine.onSave.register {
            // Save value with optional proxification.
            shell.engine.save(id, shell.engine.toProxy(current.toList()))
        }
    }

    override fun disconnect() {
        closeSave = null
    }

    override operator fun getValue(thisRef: Any?, property: KProperty<*>) =
        current.also {
            // Notify set was viewed.
            (shell.engine as? Listen)
                ?.viewed(id, it)
        }

    override fun toString() =
        current.toString()
}

/**
 * Creates a tracked property that represents a [NavigableSet] with entries which must be comparable.
 */
fun <E : Comparable<E>> set(init: () -> List<E> = ::emptyList) =
    ReadOnlyPropertyProvider { ent: Ent, property ->
        // Append to containing entity.
        val id = ent.id / property.name

        // Create set from implied values.
        SetProperty(ent.shell, id, init).also {
            // Include in entity.
            ent.driver.include(id, it)
        }
    }