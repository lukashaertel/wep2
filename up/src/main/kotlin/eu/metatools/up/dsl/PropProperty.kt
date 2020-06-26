package eu.metatools.up.dsl

import eu.metatools.up.dt.Change
import eu.metatools.up.*
import eu.metatools.up.dt.Box
import eu.metatools.up.dt.div
import eu.metatools.up.lang.ReadWritePropertyProvider
import eu.metatools.up.lang.validate
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

    override fun isChange() =
        from != to

    override fun toString() =
        "$from -> $to"
}

/**
 * Value property, uses the full [name] and the given initial value assignment [init] on non-restore.
 */
class PropProperty<T>(
    val ent: Ent,
    override val name: String,
    val init: () -> T,
    val zero: Box<T>?,
    val changed: ((PropChange<T>) -> Unit)?
) : Part, ReadWriteProperty<Any?, T> {
    private val shell get() = ent.shell

    /**
     * The actual value.
     */
    private lateinit var current: Box<T>

    override var isConnected = false
        private set

    override fun connect(partIn: PartIn?) {
        // Assign current value.
        current = if (partIn != null)
        // If loading, retrieve from the store and deproxify.
            @Suppress("unchecked_cast")
            Box(shell.toValue(partIn("current")) as T)
        else
        // Not loading, just initialize.
            Box(init())

        isConnected = true
    }

    override fun persist(partOut: PartOut) {
        // Save value content with optional proxification.
        partOut("current", shell.toProxy(current.value))
    }

    override fun disconnect() {
        isConnected = false
    }

    override operator fun getValue(thisRef: Any?, property: KProperty<*>) =
        validate(ent.driver.isConnected) {
            "Access to property in detached entity."
        } ?: current.value

    override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        // Change value.
        val previous = current.value
        current = Box(value)

        // If listening, notify changed.
        changed?.invoke(PropChange(previous, value))

        // Capture undo.
        shell.engine.capture {
            current = Box(previous)

            // If listening, notify changed back.
            changed?.invoke(PropChange(value, previous))
        }
    }

    override fun ready() {
        // Invoke initial change.
        changed?.invoke(PropChange(requireNotNull(zero).value, current.value))
    }

    override fun toString() =
        if (isConnected) current.value.toString() else "<$name, detached>"
}

/**
 * Creates a tracked property.
 */
fun <T> prop(init: () -> T) =
    ReadWritePropertyProvider { ent: Ent, property ->
        // Perform optional type check.
        Types.performTypeCheck(ent, property, false)

        // Create property from implied values.
        PropProperty(ent, property.name, init, null, null).also {
            // Include in entity.
            ent.driver.configure(it)
        }
    }

/**
 * Creates a tracked property.
 */
fun <T> propObserved(init: () -> T, zero: T, changed: (PropChange<T>) -> Unit) =
    ReadWritePropertyProvider { ent: Ent, property ->
        // Perform optional type check.
        Types.performTypeCheck(ent, property, false)

        // Create property from implied values.
        PropProperty(ent, property.name, init, Box(zero), changed).also {
            // Include in entity.
            ent.driver.configure(it)
        }
    }

/**
 * Shorthand for [prop].
 */
operator fun <T> (() -> T).provideDelegate(ent: Ent, property: KProperty<*>) =
    prop(this).provideDelegate(ent, property)