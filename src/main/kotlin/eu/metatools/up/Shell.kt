package eu.metatools.up

import eu.metatools.up.dt.*
import eu.metatools.up.lang.Bind
import kotlin.reflect.KClass

/**
 * Shell passed for [Ent]s, provides accessible methods of an [Engine].
 */
interface Shell {
    /**
     * The bound player number.
     */
    val player: Short

    /**
     * Gets the actual engine. These operations are used by [Ent]s and private methods to properly link up the entity
     * without exposing critical API.
     */
    val engine: Engine

    /**
     * Resolves an entity.
     */
    fun resolve(id: Lx): Ent?

    /**
     * Lists all entities of type, this operation returns sorted output.
     */
    fun <T : Any> list(kClass: KClass<T>): List<T>

    /**
     * The time the scope was initialized, loaded from store or set on initialize.
     */
    val initializedTime: Long

    /**
     * Provides the unified time for the given global time. Usually [Time.player] is bound by a scope.
     */
    fun time(global: Long): Time

    /**
     * Invalidates every time constrained value before the given time.
     */
    fun invalidate(global: Long)
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
 * Runs the [block] with a [Bind] on [Shell.time].
 */
inline fun Shell.withTime(global: Long, block: Bind<Time>.() -> Unit) =
    block(Bind(time(global)))

/**
 * Runs the [block] with a [Bind] on [Shell.time].
 */
inline fun Shell.withTime(clock: Clock, block: Bind<Time>.() -> Unit) =
    withTime(clock.time, block)