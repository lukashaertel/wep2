package eu.metatools.mk.tools

import eu.metatools.mk.coord.Coordinator
import eu.metatools.mk.coord.Warp
import eu.metatools.mk.track.*
import eu.metatools.mk.util.SimpleMap
import eu.metatools.mk.util.labeledAs
import eu.metatools.mk.util.toComparable
import java.lang.IllegalArgumentException


/**
 * A context for entity creation.
 * @property parent The coordinator that will be signalled to.
 * @property index The index containing all registrations/
 * @property ids The identity provider.
 */
data class Context<N, T : Comparable<T>, I>(
    val parent: Coordinator<Pair<I, N>, T>,
    val index: SimpleMap<I, Entity<N, T, I>>,
    val ids: Identifier<I>
)

/**
 * An entity, registered in an [index], dispatching signals via it's [parent] and taking it's ID from
 * the given [ids]. The entity supports [validate].
 *
 * @property context The context used to create this entity, may be used to create more entities.
 */
abstract class Entity<N, T : Comparable<T>, I>(val context: Context<N, T, I>) {
    /**
     * The ID of the entity. Automatically claimed on creation.
     */
    val id = context.ids.claim()

    init {
        // Assign index on the claimed ID to self.
        context.index[id] = this
    }

    /**
     * Deletes this entity from the index and releases the ID.
     */
    fun delete() {
        context.index.remove(id)
        context.ids.release(id)
    }

    /**
     * Sends a signal on self with the given name/time/argument triple.
     */
    fun signal(name: N, time: T, args: Any?) {
        context.parent.signal(id to name, time, args)
    }

    /**
     * Locally evaluates the instruction for an undo.
     */
    abstract fun evaluate(name: N, time: T, args: Any?): () -> Unit
}

/**
 * Uses the entities accessible by the receiver to evaluate the incoming instruction, which is keyed by
 * the entity index, as well as the name of the instruction.
 */
fun <N, T : Comparable<T>, I> SimpleMap<I, Entity<N, T, I>>.dispatchEvaluate(
    name: Pair<I, N>, time: T, args: Any?
): () -> Unit {
    // Resolve the target, return NOP if not found.
    val target = get(name.first)
        ?: return {} labeledAs { "NOP" }

    // Dispatch to entity, return it's undo.
    return target.evaluate(name.second, time, args)
}

/**
 * Small ID ([SI]) paired with a name.
 */
typealias SN<N> = Pair<SI, N>

/**
 * Big ID ([BI]) paired with a name.
 */
typealias BN<N> = Pair<BI, N>

/**
 * A map associating entities by their IDs.
 */
fun <N, T : Comparable<T>, I> entityMap() = map<I, Entity<N, T, I>>()