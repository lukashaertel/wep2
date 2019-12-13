package eu.metatools.up

import eu.metatools.up.dt.*
import eu.metatools.up.lang.Bind
import eu.metatools.up.lang.BindGenerator
import kotlin.reflect.KClass

/**
 * Shell passed for [Ent]s, provides accessible methods of an [Engine].
 */
interface Shell {
    /**
     * Gets the actual engine. These operations are used by [Ent]s and private methods to properly link up the entity
     * without exposing critical API.
     */
    val engine: Engine

    /**
     * The time the scope was initialized, loaded from store or set on initialize.
     */
    val initializedTime: Long

    /**
     * The bound player number.
     */
    val player: Short

    /**
     * Provides the unified time for the given global time. Usually [Time.player] is bound by a scope.
     */
    fun time(global: Long): Time

    /**
     * Resolves an entity.
     */
    fun resolve(id: Lx): Ent?

    /**
     * Lists all entities of type, this operation returns sorted output.
     */
    fun <T : Any> list(kClass: KClass<T>): Sequence<T>

    /**
     * Resets and loads the shell from the given [ShellIn].
     */
    fun load(shellIn: ShellIn)

    /**
     * Stores the shell to the given [shellOut]. Corresponding [load] operations must restore the state fully.
     */
    fun store(shellOut: ShellOut)

    /**
     * Dispatches an instruction with proxies.
     */
    var send: ((Instruction) -> Unit)?
        get() = throw UnsupportedOperationException("Shell does not support connection")
        set(value) = throw UnsupportedOperationException("Shell does not support connection")

    /**
     * Receives an instruction with proxies.
     */
    fun receive(instructions: Sequence<Instruction>): Unit =
        throw UnsupportedOperationException("Shell does not support connection")
}

/**
 * Performs the [Shell.load] from a [map]. If [roundTripConsistency] is given as `true`, this map is then used to check
 * consistency the output after [Shell.store] on the just restored receiver.
 */
fun Shell.loadFromMap(map: Map<Lx, Any?>, roundTripConsistency: Boolean = false) {
    // Load from map.
    load(map::get)

    // If no round-trip consistency, return.
    if (!roundTripConsistency)
        return

    // Store to check-map.
    val check = mutableMapOf<Lx, Any?>()
    storeToMap(check)

    // Get map-delta.
    val overIn = map.keys subtract check.keys
    val overOut = check.keys subtract map.keys
    val equiv = map.keys union check.keys

    // Create difference on values.
    val differing = mutableMapOf<Lx, Pair<Any?, Any?>>()
    equiv.forEach {
        val left = map.getValue(it)
        val right = check.getValue(it)
        if (left != right)
            differing[it] = left to right
    }

    // If any is not empty, throw an inconsistency exception.
    if (overIn.isNotEmpty() || overOut.isNotEmpty() || differing.isNotEmpty())
        throw InconsistencyException("Inconsistency on load", map, check, overIn, overOut, differing)
}

/**
 * Peforms the [Shell.store] to a [map].
 */
fun Shell.storeToMap(map: MutableMap<Lx, Any?>) =
    store(map::set)

/**
 * Lists all [Ent]s of type [T]
 */
inline fun <reified T : Any> Shell.list() =
    list(T::class)

/**
 * Lists all [Ent].
 */
fun Shell.listAll() =
    list(Any::class)

/**
 * Runs the [block] with a [BindGenerator] on [Shell.time].
 */
inline fun Shell.withTime(global: Long, block: Bind<Time>.() -> Unit) =
    block(BindGenerator { time(global) })

/**
 * Runs the [block] with a [BindGenerator] on [Shell.time].
 */
inline fun Shell.withTime(clock: Clock, block: Bind<Time>.() -> Unit) =
    withTime(clock.time, block)


/**
 * Converts a value to a proxy.
 */
fun Shell.toProxy(value: Any?): Any? {
    return when (value) {
        // Resolve identified object to it's Lx.
        is Ent -> value.id

        // Recursively apply to list elements.
        is List<*> -> value.mapTo(arrayListOf()) {
            toProxy(it)
        }

        // Recursively apply to array elements.
        is Array<*> -> Array(value.size) {
            toProxy(value[it])
        }

        // Recursively apply to set entries.
        is Set<*> -> value.mapTo(mutableSetOf()) {
            toProxy(it)
        }

        // Recursively apply to map entries.
        is Map<*, *> -> value.entries.associateTo(mutableMapOf()) {
            toProxy(it.key) to toProxy(it.value)
        }

        // Recursively apply to triple entries.
        is Triple<*, *, *> -> Triple(toProxy(value.first), toProxy(value.second), toProxy(value.third))

        // Recursively apply to pair entries.
        is Pair<*, *> -> Pair(toProxy(value.first), toProxy(value.second))

        // Return just the value.
        else -> value
    }
}

/**
 * Converts a proxy to a value.
 */
fun Shell.toValue(proxy: Any?): Any? {
    return when (proxy) {
        // Resolve Lx to identified object.
        is Lx -> resolve(proxy)

        // Recursively apply to list elements.
        is List<*> -> proxy.mapTo(arrayListOf()) {
            toValue(it)
        }

        // Recursively apply to array elements.
        is Array<*> -> Array(proxy.size) {
            toValue(proxy[it])
        }

        // Recursively apply to set entries.
        is Set<*> -> proxy.mapTo(mutableSetOf()) {
            toValue(it)
        }

        // Recursively apply to map entries.
        is Map<*, *> -> proxy.entries.associateTo(mutableMapOf()) {
            toValue(it.key) to toValue(it.value)
        }

        // Recursively apply to triple entries.
        is Triple<*, *, *> -> Triple(toValue(proxy.first), toValue(proxy.second), toValue(proxy.third))

        // Recursively apply to pair entries.
        is Pair<*, *> -> Pair(toValue(proxy.first), toValue(proxy.second))

        // Return just the value.
        else -> proxy
    }
}

/**
 * Converts the instruction to a value instruction via [Shell.toValue].
 */
fun Instruction.toValueWith(shell: Shell) =
    Instruction(target, methodName, time, args.map(shell::toValue))

/**
 * Converts the instruction to a proxy instruction via [Shell.toProxy].
 */
fun Instruction.toProxyWith(shell: Shell) =
    Instruction(target, methodName, time, args.map(shell::toProxy))

/**
 * Runs the receive method with the var-arg list.
 */
fun Shell.receive(vararg instructions: Instruction) =
    receive(instructions.asSequence())