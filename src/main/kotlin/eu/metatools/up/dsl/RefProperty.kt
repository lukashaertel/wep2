package eu.metatools.up.dsl

import eu.metatools.up.Ent
import eu.metatools.up.Part
import eu.metatools.up.Shell
import eu.metatools.up.dt.Lx
import eu.metatools.up.dt.div
import eu.metatools.up.lang.ReadOnlyPropertyProvider
import eu.metatools.up.lang.autoClosing
import eu.metatools.up.lang.validate
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.safeCast

/**
 * A fixed reference, pointing to an entity of [type]. If not resolved or not of the required type, [getValue] will
 * throw an error.
 */
class RefProperty<T : Ent>(
    val ent: Ent, val id: Lx, val ref: Lx, val type: KClass<T>
) : Part, ReadOnlyProperty<Any?, T> {
    private val shell get() = ent.shell

    /**
     * The actual value.
     */
    private var current: T? = null

    /**
     * Close handle for resolve connection.
     */
    private var closeAdd by autoClosing()
    private var closeRemove by autoClosing()

    override var isConnected = false
        private set

    override fun connect() {
        // Assign current value.
        current = type.safeCast(shell.resolve(ref))

        // Register resolution methods.
        closeAdd = shell.engine.onAdd.register(ref) {
            current = type.safeCast(it)
        }
        closeRemove = shell.engine.onRemove.register(ref) {
            current = null
        }

        isConnected = true
    }

    override fun disconnect() {
        isConnected = false

        closeAdd = null
    }

    override operator fun getValue(thisRef: Any?, property: KProperty<*>) =
        validate(ent.driver.isConnected) {
            "Access to ref in detached entity."
        } ?: current ?: error("Pointer reference not assigned or not of type $type.")

    override fun toString() =
        if (isConnected) current.toString() else "<$id, detached>"
}

/**
 * Creates a reference property.
 */
inline fun <reified T : Ent> ref(ref: Lx) =
    ReadOnlyPropertyProvider { ent: Ent, property ->
        // Append to containing entity.
        val id = ent.id / property.name

        RefProperty(ent, id, ref, T::class).also {
            // Include in entity.
            ent.driver.include(id, it)
        }
    }