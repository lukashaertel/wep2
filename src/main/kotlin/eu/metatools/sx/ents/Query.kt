package eu.metatools.sx.ents

import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.safeCast

interface Query {
    /**
     * Lists all required actors.
     */
    fun all(world: World): SortedSet<Actor<*, *>>

    /**
     * True if this actor is considered.
     */
    operator fun contains(actor: Actor<*, *>): Boolean
}

/**
 * Queries all actors with a predicate.
 * @property predicate The predicate on state and delta.
 */
data class QueryWhere(val predicate: (DefActor<*, *>, Any?, Any?) -> Boolean) : Query {
    constructor(predicate: (Any?, Any?) -> Boolean) : this({ _, state, delta -> predicate(state, delta) })

    override fun all(world: World): SortedSet<Actor<*, *>> {
        return world.actors
            .asSequence()
            .filterTo(TreeSet()) { predicate(it.inDef, it.state, it.delta) }
    }

    override fun contains(actor: Actor<*, *>): Boolean {
        return predicate(actor.inDef, actor.state, actor.delta)
    }
}

/**
 * Returns nothing.
 */
object QueryNone : Query {
    override fun all(world: World) =
        sortedSetOf<Actor<*, *>>()

    override fun contains(actor: Actor<*, *>) =
        false
}