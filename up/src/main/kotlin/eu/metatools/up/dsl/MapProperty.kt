package eu.metatools.up.dsl

import eu.metatools.up.*
import eu.metatools.up.dt.Change
import eu.metatools.up.lang.ObservedMap
import eu.metatools.up.lang.ReadOnlyPropertyProvider
import eu.metatools.up.lang.aligned
import eu.metatools.up.lang.validate
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
     * The comparator used for sorting.
     */
    val comparator: Comparator<in K>? = added.comparator() ?: removed.comparator()

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
        added.filterTo(TreeMap(comparator)) { (k, _) ->
            k !in removed
        }
    }

    /**
     * The changed elements with their old and new values.
     */
    val changed by lazy {
        TreeMap<K, Pair<V, V>>(comparator).apply {
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
        removed.filterTo(TreeMap(comparator)) { (k, _) ->
            k !in added
        }
    }

    override fun merge(other: MapChange<K, V>) =
        MapChange<K, V>(
            // A' union (A subtract R')
            added.filterTo(TreeMap<K, V>(comparator)) { (k, v) ->
                other.removed[k] != v
            }.also {
                it.putAll(other.added)
            },
            // R union (R' subtract A)
            other.removed.filterTo(TreeMap<K, V>(comparator)) { (k, v) ->
                added[k] != v
            }.also {
                it.putAll(removed)
            }
        )

    override fun invert() =
        MapChange(removed, added)

    override fun isChange() =
        removed.isNotEmpty() || added.isNotEmpty()

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
 * Map property, uses the full [name] and the given initial value assignment [init] on non-restore..
 */
class MapProperty<K, V>(
    override val name: String,
    val comparator: Comparator<in K>?,
    val ent: Ent,
    val init: () -> Map<K, V>,
    val changed: ((MapChange<K, V>) -> Unit)?
) : Part, ReadOnlyProperty<Any?, NavigableMap<K, V>> {
    private val shell get() = ent.shell

    /**
     * The observed map dealing with notification of changes.
     */
    private lateinit var current: ObservedMap<K, V>

    private fun createObservedMap(from: Map<K, V>) =
        ObservedMap(TreeMap<K, V>(comparator).apply { putAll(from) }) { add, remove ->
            // If listening, notify changed.
            changed?.invoke(MapChange(add, remove))

            // Capture undo.
            shell.engine.capture {
                // Undo changes properly.
                current.actual.entries.removeAll(add.entries)
                current.actual.putAll(remove)

                // If listening, notify changed back.
                changed?.invoke(MapChange(remove, add))
            }
        }

    override var isConnected = false
        private set

    override fun connect(partIn: PartIn?) {
        // Load from store and register saving if needed.
        current = if (partIn != null)
        // If loading, retrieve from the store and deproxify.
            @Suppress("unchecked_cast")
            createObservedMap(shell.toValue(partIn("current")) as Map<K, V>)
        else
        // Not loading, just initialize.
            createObservedMap(init())

        isConnected = true
    }

    override fun persist(partOut: PartOut) {
        // Save value with optional proxification.
        partOut("current", shell.toProxy(current))
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
        changed?.invoke(MapChange(TreeMap(current), current.aligned()))
    }

    override fun toString() =
        if (isConnected) current.toString() else "<$name, detached>"
}

/**
 * Creates a tracked property that represents a [NavigableMap] with keys which must be comparable.
 */
fun <K : Comparable<K>, V> map(
    comparator: Comparator<in K>?,
    init: () -> Map<K, V> = ::emptyMap
) = ReadOnlyPropertyProvider { ent: Ent, property ->
    // Perform optional type check.
    Types.performTypeCheck(ent, property, true)

    // Create map from implied values.
    MapProperty(property.name, comparator, ent, init, null).also {
        // Include in entity.
        ent.driver.configure(it)
    }
}

/**
 * Creates a tracked property that represents a [NavigableMap] with keys which must be comparable.
 */
fun <K : Comparable<K>, V> map(init: () -> Map<K, V> = ::emptyMap) =
    map(null, init)

/**
 * Creates a tracked property that represents a [NavigableMap] with keys which must be comparable.
 */
fun <K : Comparable<K>, V> mapObserved(
    comparator: Comparator<in K>?,
    init: () -> Map<K, V> = ::emptyMap,
    changed: (MapChange<K, V>) -> Unit
) = ReadOnlyPropertyProvider { ent: Ent, property ->
    // Perform optional type check.
    Types.performTypeCheck(ent, property, true)

    // Create map from implied values.
    MapProperty(property.name, comparator, ent, init, changed).also {
        // Include in entity.
        ent.driver.configure(it)
    }
}

/**
 * Creates a tracked property that represents a [NavigableMap] with keys which must be comparable.
 */
fun <K : Comparable<K>, V> mapObserved(init: () -> Map<K, V> = ::emptyMap, changed: (MapChange<K, V>) -> Unit) =
    mapObserved(null, init, changed)