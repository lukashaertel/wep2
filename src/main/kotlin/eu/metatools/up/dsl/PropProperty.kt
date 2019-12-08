package eu.metatools.up.dsl

import eu.metatools.up.dt.Change
import eu.metatools.up.*
import eu.metatools.up.dt.Box
import eu.metatools.up.dt.Lx
import eu.metatools.up.dt.div
import eu.metatools.up.lang.autoClosing
import eu.metatools.up.lang.ReadWritePropertyProvider
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

    override fun toString() =
        "$from -> $to"
}

// TODO: Changed delegate invocation on init.

/**
 * Value property, uses the full [id] and the given initial value assignment [init] on non-restore.
 */
class PropProperty<T>(
    val shell: Shell, val id: Lx, val init: () -> T, val changed: ((PropChange<T>) -> Unit)?
) : Part, ReadWriteProperty<Any?, T> {

    /**
     * The actual value.
     */
    private lateinit var current: Box<T>

    /**
     * Close handle for store connection.
     */
    private var closeSave by autoClosing()

    override fun connect() {
        // Assign current value.
        current = if (shell.engine.isLoading)
        // If loading, retrieve from the store and deproxify.
            @Suppress("unchecked_cast")
            Box(shell.toValue(shell.engine.load(id)) as T)
        else
        // Not loading, just initialize.
            Box(init())

        // Register saving method.
        closeSave = shell.engine.onSave.register {
            // Save value content with optional proxification.
            shell.engine.save(id, shell.toProxy(current.value))
        }
    }

    override fun disconnect() {
        closeSave = null
    }

    override operator fun getValue(thisRef: Any?, property: KProperty<*>) =
        current.value

    override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        // Change value.
        val previous = current.value
        current = Box(value)

        // If listening, notify changed.
        changed?.invoke(PropChange(previous, value))

        // Capture undo.
        shell.engine.capture(id) {
            current = Box(previous)

            // If listening, notify changed back.
            changed?.invoke(PropChange(value, previous))
        }
    }

    override fun toString() =
        current.value.toString()
}

/**
 * Creates a tracked property.
 */
fun <T> prop(init: () -> T) =
    ReadWritePropertyProvider { ent: Ent, property ->
        // Append to containing entity.
        val id = ent.id / property.name

        // Create property from implied values.
        PropProperty(ent.shell, id, init, null).also {
            // Include in entity.
            ent.driver.include(id, it)
        }
    }

/**
 * Creates a tracked property.
 */
fun <T> propObserved(init: () -> T, changed: (PropChange<T>) -> Unit) =
    ReadWritePropertyProvider { ent: Ent, property ->
        // Append to containing entity.
        val id = ent.id / property.name

        // Create property from implied values.
        PropProperty(ent.shell, id, init, changed).also {
            // Include in entity.
            ent.driver.include(id, it)
        }
    }

/**
 * Shorthand for [prop].
 */
inline operator fun <reified T> (() -> T).provideDelegate(ent: Ent, property: KProperty<*>) =
    prop(this).provideDelegate(ent, property)