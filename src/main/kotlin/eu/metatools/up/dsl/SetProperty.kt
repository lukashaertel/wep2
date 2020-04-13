package eu.metatools.up.dsl

import eu.metatools.up.*
import eu.metatools.up.dt.Change
import eu.metatools.up.lang.ObservedSet
import eu.metatools.up.lang.ReadOnlyPropertyProvider
import eu.metatools.up.lang.validate
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
 * Set property, uses the full [name] and the given initial value assignment [init] on non-restore.
 */
class SetProperty<E : Comparable<E>>(
    val ent: Ent, override val name: String, val init: () -> List<E>, val changed: ((SetChange<E>) -> Unit)?
) : Part, ReadOnlyProperty<Any?, NavigableSet<E>> {
    private val shell get() = ent.shell

    /**
     * The observed set dealing with notification of changes.
     */
    private lateinit var current: ObservedSet<E>

    override var isConnected = false
        private set

    private fun createObservedSet(from: List<E>) =
        ObservedSet(TreeSet(from)) { add, remove ->
            // If listening, notify changed.
            changed?.invoke(SetChange(add, remove))

            shell.engine.capture {
                // Undo changes properly.
                current.actual.removeAll(add)
                current.actual.addAll(remove)

                // If listening, notify changed back.
                changed?.invoke(SetChange(remove, add))
            }
        }

    override fun connect(partIn: PartIn?) {
        // Assign current value.
        current = if (partIn != null)
        // If loading, retrieve from the store and deproxify.
            @Suppress("unchecked_cast")
            createObservedSet(shell.toValue(partIn("current")) as List<E>)
        else
        // Not loading, just initialize.
            createObservedSet(init())

        isConnected = true
    }

    override fun persist(partOut: PartOut) {
        // Save value with optional proxification.
        partOut("current", shell.toProxy(current.toList()))
    }

    override fun disconnect() {
        isConnected = false
    }

    override fun ready() {
        // Invoke initial change.
        changed?.invoke(SetChange(current.toSortedSet(), sortedSetOf()))
    }

    override operator fun getValue(thisRef: Any?, property: KProperty<*>) =
        validate(ent.driver.isConnected) {
            "Access to set in detached entity."
        } ?: current

    override fun toString() =
        if (isConnected) current.toString() else "<$name, detached>"
}

/**
 * Creates a tracked property that represents a [NavigableSet] with entries which must be comparable.
 */
fun <E : Comparable<E>> set(init: () -> List<E> = ::emptyList) =
    ReadOnlyPropertyProvider { ent: Ent, property ->
        // Perform optional type check.
        Types.performTypeCheck(ent, property, true)

        // Create set from implied values.
        SetProperty(ent, property.name, init, null).also {
            // Include in entity.
            ent.driver.configure(it)
        }
    }

/**
 * Creates a tracked property that represents a [NavigableSet] with entries which must be comparable.
 */
fun <E : Comparable<E>> setObserved(init: () -> List<E> = ::emptyList, changed:  (SetChange<E>) -> Unit) =
    ReadOnlyPropertyProvider { ent: Ent, property ->
        // Perform optional type check.
        Types.performTypeCheck(ent, property, true)

        // Create set from implied values.
        SetProperty(ent, property.name, init, changed).also {
            // Include in entity.
            ent.driver.configure(it)
        }
    }