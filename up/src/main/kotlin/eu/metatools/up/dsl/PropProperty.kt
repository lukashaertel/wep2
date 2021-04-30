package eu.metatools.up.dsl

import eu.metatools.up.*
import eu.metatools.up.dt.Box
import eu.metatools.up.dt.Change
import eu.metatools.up.lang.ReadWritePropertyProvider
import eu.metatools.up.lang.validate
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.reflect.jvm.isAccessible

sealed class PropChange<T> : Change<PropChange<T>> {
    abstract val from: T
    abstract val to: T
}

/**
 * Initialization change on a [PropProperty].
 * @property to The initialized value.
 */
data class PropInit<T>(override val to: T) : PropChange<T>() {
    /**
     * Returns the value of [to].
     */
    override val from: T
        get() = to

    override fun merge(other: PropChange<T>) =
        PropInit(other.to)

    override fun invert() = throw UnsupportedOperationException("Unsupported inversion.")

    override fun isChange() =
        true

    override fun toString() =
        "(*) $to"
}

/**
 * A [Change] on a [PropProperty].
 * @property from The original value.
 * @property to The new value.
 */
data class PropAssign<T>(override val from: T, override val to: T) : PropChange<T>() {
    override fun merge(other: PropChange<T>) =
        PropAssign(from, other.to)

    override fun invert() =
        PropAssign(to, from)

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
    val init: () -> T
) : Part, ReadWriteProperty<Any?, T> {
    /**
     * Reference to the containing [Ent]'s shell.
     */
    private val shell get() = ent.shell

    /**
     * The actual value.
     */
    private lateinit var current: Box<T>

    override var isConnected = false
        private set

    override lateinit var notifyHandle: (name: String, change: Change<*>) -> Unit

    var changed: ((PropChange<T>) -> Unit) = {}

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

        // Create change object.
        val changeSet = PropAssign(previous, value)

        // If listening, notify changed.
        changed?.invoke(changeSet)
        notifyHandle(name, changeSet)

        // Capture undo.
        shell.engine.capture {
            current = Box(previous)

            // Create reset change object.
            val changeReset = PropAssign(value, previous)

            // If listening, notify changed back.
            changed.invoke(changeReset)
            notifyHandle(name, changeReset)
        }
    }

    override fun ready() {
        // Create ready change.
        val propReady = PropInit(current.value)

        // Invoke initial change.
        changed.invoke(propReady)
        notifyHandle(name, propReady)
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
        PropProperty(ent, property.name, init).also {
            // Include in entity.
            ent.driver.configure(it)
        }
    }

/**
 * Shorthand for [prop].
 */
operator fun <T> (() -> T).provideDelegate(ent: Ent, property: KProperty<*>) =
    prop(this).provideDelegate(ent, property)

/**
 * Adds a listener to a property that was defined as a [prop].
 * @param handler The change handler.
 */
fun <T> KProperty0<T>.listenProp(handler: (PropChange<T>) -> Unit) {
    // Memorize for clean-up.
    val isAccessibleBefore = isAccessible

    try {
        // Set accessible and get delegate.
        isAccessible = true
        val target = getDelegate()

        // Require to be delegate of appropriate type.
        require(target is PropProperty<*>) { "Receiver $this is not an observable property." }

        // Type assert inner.
        @Suppress("unchecked_cast")
        target as PropProperty<T>

        // Append handler.
        val before = target.changed
        target.changed = {
            before(it)
            handler(it)
        }
    } finally {
        // Reset accessibility.
        isAccessible = isAccessibleBefore
    }
}