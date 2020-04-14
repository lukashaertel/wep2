package eu.metatools.sx.ents

import eu.metatools.fio.data.Tri
import eu.metatools.sx.SX
import eu.metatools.sx.index.*
import eu.metatools.up.Ent
import eu.metatools.up.Shell
import eu.metatools.up.dsl.*
import eu.metatools.up.dt.Lx
import eu.metatools.up.dt.lx


/**
 * Known index.
 */
interface KI<K : Comparable<K>> {
    /**
     * Unique key of index.
     */
    val key: Lx

    /**
     * Definition if index is absent.
     */
    val definition: Index<K, Any?>
}

class Actor(
    shell: Shell,
    id: Lx,
    sx: SX,
    inLocation: Tri,
    inState: Any
) : Ent(shell, id) {
    var onLocationChange: ((Actor, Tri, Tri) -> Unit)? = null
    var onStateChange: ((Actor, Any, Any) -> Unit)? = null

    override val extraArgs = mapOf(
        "inLocation" to inLocation,
        "inState" to inState
    )
    var location by propObserved({ inLocation }, inLocation) {
        if (it.isChange())
            onLocationChange?.invoke(this, it.from, it.to)
    }
    var state by propObserved({ inState }, inState) {
        if (it.isChange())
            onStateChange?.invoke(this, it.from, it.to)
    }

}

class World(
    shell: Shell,
    id: Lx,
    sx: SX
) : Ent(shell, lx) {
    val players by set<Player>()

    private val indices by map<Lx, Pair<Index<*, Any?>, Int>>()

    /**
     * Increases the ref-count of the given index definition and returns the index.
     */
    fun <K : Comparable<K>> acquire(source: KI<K>): Index<K, Any?> {
        // Update reference on index.
        @Suppress("unchecked_cast")
        return requireNotNull(indices.compute(source.key) { _, existing ->
            if (existing != null)
            // Existing index, increment references.
                existing.first to existing.second.inc()
            else
            // Non-existing index, create and initialize.
                source.definition as Index<*, Any?> to 1
        }).first as Index<K, Any?>
    }

    /**
     * Decreases the ref-count of the given index definition or removes the definition.
     */
    fun release(source: KI<*>) {
        // Decrement and delete reference.
        indices.compute(source.key) { _, existing ->
            if (existing != null && 1 < existing.second)
            // Existing and reference count valid after decrement, decrement reference.
                existing.first to existing.second.dec()
            else
            // Invalid, delete.
                null
        }
    }

    val actors= IndexVolume<Actor>()

    private val actorSet by setObserved<Actor> { (added, removed) ->
        for (actor in removed) {
            actor.onLocationChange = null
            actor.onStateChange = null
        }

        for (actor in added) {
            actor.onLocationChange = ::updateLocation
            actor.onStateChange = ::updateState
        }
    }

    private fun updateLocation(actor: Actor, from: Tri, to: Tri) {

    }

    private fun updateState(actor: Actor, from: Any, to: Any) {

    }

    /**
     * Periodic world update.
     */
    val worldUpdate = repeating(Short.MAX_VALUE, 1000, shell::initializedTime) {

    }


    /**
     * Renders all actors.
     */
    fun render(time: Double, delta: Double) {
        //for (actor in actors)
        // actor.render(time, delta)
    }
}