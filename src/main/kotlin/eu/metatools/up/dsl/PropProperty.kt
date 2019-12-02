package eu.metatools.up.dsl

import eu.metatools.elt.Change
import eu.metatools.elt.Listen
import eu.metatools.up.Ent
import eu.metatools.up.Mode
import eu.metatools.up.Scope
import eu.metatools.up.dt.Box
import eu.metatools.up.dt.Lx
import eu.metatools.up.dt.div
import eu.metatools.up.lang.autoClosing
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

    override fun toString() =
        "$from -> $to"
}

class PropProperty<T>(
    val scope: Scope,  val id: Lx, val init: () -> T
) :  Part, ReadWriteProperty<Any?, T> {

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
        current = if (scope.mode == Mode.RestoreData)
        // If loading, retrieve from the store and deproxify.
            Box(scope.toValue(scope.load(id)) as T)
        else
        // Not loading, just initialize.
            Box(init())

        // Register saving method.
        closeSave = scope.onSave.register {
            // Save value content with optional proxification.
            scope.save(id, scope.toProxy(current.value))
        }
    }

    override fun disconnect() {
        closeSave = null
    }

    override operator fun getValue(thisRef: Any?, property: KProperty<*>) =
        current.value.also {
            // Notify property was viewed.
            if (scope is Listen)
                scope.viewed(id, it)
        }

    override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        // Change value.
        val previous = current.value
        current = Box(value)

        // If listening, notify changed.
        if (scope is Listen)
            scope.changed(id, PropChange(previous, value))

        // Capture undo.
        scope.capture(id) {
            current = Box(previous)

            // If listening, notify changed back.
            if (scope is Listen)
                scope.changed(id, PropChange(value, previous))
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
    ReadWritePropertyProvider { ent: Ent, property ->
        // Append to containing entity.
        val id = ent.id / property.name

        // Create property from implied values.
        PropProperty(ent.scope, id, init).also {
            // Include in entity.
            ent.include(id, it)
        }
    }