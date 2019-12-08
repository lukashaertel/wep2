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
    fun <T : Any> list(kClass: KClass<T>): List<T>
}


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
