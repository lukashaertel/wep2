package eu.metatools.up.dsl

import eu.metatools.up.*
import eu.metatools.up.dt.Change
import eu.metatools.up.lang.ObservedSet
import eu.metatools.up.lang.ReadOnlyPropertyProvider
import eu.metatools.up.lang.aligned
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
    /**
     * The comparator used for sorting.
     */
    val comparator: Comparator<in E>? = added.comparator() ?: removed.comparator()

    override fun merge(other: SetChange<E>) =
        SetChange(
            // A' union (A subtract R')
            TreeSet(comparator).apply { addAll(other.added union (added subtract other.removed)) },
            // R union (R' subtract A)
            TreeSet(comparator).apply { addAll(removed union (other.removed subtract added)) }
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

    override fun isChange() =
        removed.isNotEmpty() || added.isNotEmpty()

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
class SetProperty<E>(
    override val name: String,
    val comparator: Comparator<in E>?,
    val ent: Ent,
    val init: () -> List<E>,
    val changed: ((SetChange<E>) -> Unit)?
) : Part, ReadOnlyProperty<Any?, NavigableSet<E>> {
    private val shell get() = ent.shell

    /**
     * The observed set dealing with notification of changes.
     */
    private lateinit var current: ObservedSet<E>

    override var isConnected = false
        private set

    override lateinit var notifyHandle: (name: String, change: Change<*>) -> Unit

    private fun createObservedSet(from: List<E>) =
        ObservedSet(TreeSet(comparator).apply { addAll(from) }) { add, remove ->
            // Create change object.
            val changeSet = SetChange(add, remove)

            // If listening, notify changed.
            changed?.invoke(changeSet)
            notifyHandle(name, changeSet)


            shell.engine.capture {
                // Undo changes properly.
                current.actual.removeAll(add)
                current.actual.addAll(remove)

                // Create reset change object.
                val changeReset = SetChange(remove, add)

                // If listening, notify changed back.
                changed?.invoke(changeReset)
                notifyHandle(name, changeReset)
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
        // Create ready change.
        val propReady= SetChange(TreeSet(current), current.aligned())

        // Invoke initial change.
        changed?.invoke(propReady)
        notifyHandle(name, propReady)
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
fun <E : Comparable<E>> set(
    comparator: Comparator<in E>?,
    init: () -> List<E> = ::emptyList
) = ReadOnlyPropertyProvider { ent: Ent, property ->
    // Perform optional type check.
    Types.performTypeCheck(ent, property, true)

    // Create set from implied values.
    SetProperty(property.name, comparator, ent, init, null).also {
        // Include in entity.
        ent.driver.configure(it)
    }
}

/**
 * Creates a tracked property that represents a [NavigableSet] with entries which must be comparable.
 */
fun <E : Comparable<E>> set(init: () -> List<E> = ::emptyList) = set(null, init)

/**
 * Creates a tracked property that represents a [NavigableSet] with entries which must be comparable.
 */
fun <E : Comparable<E>> setObserved(
    comparator: Comparator<in E>? = null,
    init: () -> List<E> = ::emptyList,
    changed: (SetChange<E>) -> Unit
) = ReadOnlyPropertyProvider { ent: Ent, property ->
    // Perform optional type check.
    Types.performTypeCheck(ent, property, true)

    // Create set from implied values.
    SetProperty(property.name, comparator, ent, init, changed).also {
        // Include in entity.
        ent.driver.configure(it)
    }
}

/**
 * Creates a tracked property that represents a [NavigableSet] with entries which must be comparable.
 */
fun <E : Comparable<E>> setObserved(init: () -> List<E> = ::emptyList, changed: (SetChange<E>) -> Unit) =
    setObserved(null, init, changed)