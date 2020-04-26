package eu.metatools.sx.data

import eu.metatools.fio.data.Tri
import eu.metatools.up.*
import eu.metatools.up.dsl.MapChange
import eu.metatools.up.dsl.Types
import eu.metatools.up.lang.ReadOnlyPropertyProvider
import eu.metatools.up.lang.validate
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class VolumeProperty<V : Any>(
    override val name: String,
    val ent: Ent,
    val init: () -> Map<Tri, V>,
    val changed: ((MapChange<Tri, V>) -> Unit)?
) : Part,
    ReadOnlyProperty<Any?, Volume<V>> {
    private val shell get() = ent.shell

    override var isConnected = false
        private set

    private lateinit var current: ObservedVolume<V>

    private fun createVolume(from: Map<Tri, V>) =
        ObservedVolume(VolumeValues<V>().apply { assign(from) }) { add, remove ->
            // If listening, notify changed.
            changed?.invoke(MapChange(add, remove))

            // Capture undo.
            shell.engine.capture {
                // Undo changes properly.
                add.forEach { (at, _) -> current.actual.remove(at.x, at.y, at.z) }
                current.actual.assign(remove)

                // If listening, notify changed back.
                changed?.invoke(MapChange(remove, add))
            }
        }

    override fun connect(partIn: PartIn?) {
        // Load from store and register saving if needed.
        current = if (partIn != null)
        // If loading, retrieve from the store and deproxify.
            @Suppress("unchecked_cast")
            createVolume(shell.toValue(partIn("current")) as Map<Tri, V>)
        else
        // Not loading, just initialize.
            createVolume(init())

        isConnected = true
    }

    override fun persist(partOut: PartOut) {
        // Save value with optional proxification.
        partOut("current", shell.toProxy(current.toMap()))
    }

    override fun disconnect() {
        isConnected = false
    }

    override operator fun getValue(thisRef: Any?, property: KProperty<*>) =
        validate(ent.driver.isConnected) {
            "Access to property in detached entity."
        } ?: current

    override fun ready() {
        // Invoke initial change.
        changed?.invoke(MapChange(current.toSortedMap(), sortedMapOf()))
    }

    override fun toString() =
        if (isConnected) current.toString() else "<$name, detached>"
}

/**
 * Creates a tracked property that represents a [Volume].
 */
fun <V : Any> volume(
    init: () -> Map<Tri, V> = ::emptyMap
) = ReadOnlyPropertyProvider { ent: Ent, property ->
    // Perform optional type check.
    Types.performTypeCheck(ent, property, true)

    // Create map from implied values.
    VolumeProperty(property.name, ent, init, null).also {
        // Include in entity.
        ent.driver.configure(it)
    }
}

/**
 * Creates a tracked property that represents a [Volume].
 */
fun <V : Any> volumeObserved(
    init: () -> Map<Tri, V> = ::emptyMap, changed: (MapChange<Tri, V>) -> Unit
) = ReadOnlyPropertyProvider { ent: Ent, property ->
    // Perform optional type check.
    Types.performTypeCheck(ent, property, true)

    // Create map from implied values.
    VolumeProperty(property.name, ent, init, changed).also {
        // Include in entity.
        ent.driver.configure(it)
    }
}
