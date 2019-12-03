package eu.metatools.up.dsl

import eu.metatools.elt.Change
import eu.metatools.elt.Listen
import eu.metatools.up.*
import eu.metatools.up.dt.Lx
import eu.metatools.up.dt.div
import eu.metatools.up.lang.ObservedMap
import eu.metatools.up.lang.autoClosing
import eu.metatools.up.Mode
import eu.metatools.wep2.util.delegates.ReadOnlyPropertyProvider
import java.util.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * A [Change] on a [MapProperty].
 * @property added The added entries.
 * @property removed The removed entries.
 */
data class MapChange<K, V>(
    val added: SortedMap<K, V>,
    val removed: SortedMap<K, V>
) : Change<MapChange<K, V>> {
    /**
     * Constructs a [MapChange] from the secondary defining values. Insertions and deletions
     * that are also mentioned as changes are ignored.
     */
    constructor(
        inserted: SortedMap<K, V>,
        changed: SortedMap<K, Pair<V, V>>,
        deleted: SortedMap<K, V>
    ) : this(
        // Override inserted values with change ends.
        changed.entries.associateTo(TreeMap(inserted)) {
            it.key to it.value.second
        },
        // Override deleted values with change starts.
        changed.entries.associateTo(TreeMap(deleted)) {
            it.key to it.value.first
        }
    )

    /**
     * The added elements that are not part of a change.
     */
    val inserted by lazy {
        added.filterTo(TreeMap()) { (k, _) ->
            k !in removed
        }
    }

    /**
     * The changed elements with their old and new values.
     */
    val changed by lazy {
        TreeMap<K, Pair<V, V>>().apply {
            for ((k, old) in removed)
                added[k]?.let { new ->
                    put(k, old to new)
                }
        }
    }

    /**
     * The removed elements that are not part of a change.
     */
    val deleted by lazy {
        removed.filterTo(TreeMap()) { (k, _) ->
            k !in added
        }
    }

    override fun merge(other: MapChange<K, V>) =
        MapChange<K, V>(
            // A' union (A subtract R')
            added.filterTo(TreeMap<K, V>()) { (k, v) ->
                other.removed[k] != v
            }.also {
                it.putAll(other.added)
            },
            // R union (R' subtract A)
            other.removed.filterTo(TreeMap<K, V>()) { (k, v) ->
                added[k] != v
            }.also {
                it.putAll(removed)
            }
        )

    override fun invert() =
        MapChange(removed, added)

    /**
     * Applies the change to a map.
     */
    fun apply(map: Map<K, V>) =
        // Compose via set arithmetic.
        (map - removed) + added

    /**
     * Applies the change to a map.
     */
    fun applyTo(map: MutableMap<K, V>) {
        // Remove all removed entries.
        map.entries.removeAll(map.entries)

        // Add all added entries.
        map.putAll(added)
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
 * Map property, uses the full [id] and the given initial value assignment [init] on non-restore..
 */
class MapProperty<K : Comparable<K>, V>(
    val shell: Shell, val id: Lx, val init: () -> Map<K, V>, val changed: ((MapChange<K, V>) -> Unit)?
) : Part, ReadOnlyProperty<Any?, NavigableMap<K, V>> {

    /**
     * The observed map dealing with notification of changes.
     */
    private lateinit var current: ObservedMap<K, V>

    /**
     * Close handle for store connection.
     */
    private var closeSave by autoClosing()

    private fun createObservedMap(from: Map<K, V>) =
        ObservedMap(TreeMap(from)) { add, remove ->
            // If listening, notify changed.
            changed?.invoke(MapChange(add, remove))

            // Capture undo.
            shell.engine.capture(id) {
                // Undo changes properly.
                current.actual.entries.removeAll(add.entries)
                current.actual.putAll(remove)

                // If listening, notify changed back.
                changed?.invoke(MapChange(remove, add))
            }
        }

    override fun connect() {

        // Load from store and register saving if needed.
        current = if (shell.engine.mode == Mode.RestoreData)
        // If loading, retrieve from the store and deproxify.
            @Suppress("unchecked_cast")
            createObservedMap(shell.engine.toValue(shell.engine.load(id)) as Map<K, V>)
        else
        // Not loading, just initialize.
            createObservedMap(init())

        // Register saving method.
        closeSave = shell.engine.onSave.register {
            // Save value with optional proxification.
            shell.engine.save(id, shell.engine.toProxy(current))
        }
    }

    override fun disconnect() {
        closeSave = null
    }

    override operator fun getValue(thisRef: Any?, property: KProperty<*>) =
        current

    override fun toString() =
        current.toString()
}

/**
 * Creates a tracked property that represents a [NavigableMap] with keys which must be comparable.
 */
fun <K : Comparable<K>, V> map(init: () -> Map<K, V> = ::emptyMap) =
    ReadOnlyPropertyProvider { ent: Ent, property ->
        // Append to containing entity.
        val id = ent.id / property.name

        // Create map from implied values.
        MapProperty(ent.shell, id, init, null).also {
            // Include in entity.
            ent.driver.include(id, it)
        }
    }

/**
 * Creates a tracked property that represents a [NavigableMap] with keys which must be comparable.
 */
fun <K : Comparable<K>, V> mapObserved(init: () -> Map<K, V> = ::emptyMap, changed: (MapChange<K, V>) -> Unit) =
    ReadOnlyPropertyProvider { ent: Ent, property ->
        // Append to containing entity.
        val id = ent.id / property.name

        // Create map from implied values.
        MapProperty(ent.shell, id, init, changed).also {
            // Include in entity.
            ent.driver.include(id, it)
        }
    }