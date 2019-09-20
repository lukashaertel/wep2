package eu.metatools.wep2.entity

import eu.metatools.wep2.coord.Coordinator
import eu.metatools.wep2.entity.bind.Restore
import eu.metatools.wep2.entity.bind.Store
import eu.metatools.wep2.track.*
import eu.metatools.wep2.util.*

/**
 * A context for entity creation.
 */
interface Context<N, T : Comparable<T>, I : Comparable<I>> {
    /**
     * The index containing all registrations.
     */
    val index: SimpleMap<I, Entity<N, T, I>>

    fun newId(): I

    fun releaseId(id: I)

    /**
     * Signals dispatched via [identity].
     */
    fun signal(identity: I, name: N, time: T, args: Any?)
}

/**
 * Creates a context from the given implementations.
 * @param index The index to locate and store entities in.
 * @param newId The method of generating a new ID.
 * @param releaseId The method of releasing an ID.
 * @param signal The dispatch method, per entity ID and local name.
 */
inline fun <N, T : Comparable<T>, I : Comparable<I>> context(
    index: SimpleMap<I, Entity<N, T, I>>,
    crossinline newId: () -> I,
    crossinline releaseId: (I) -> Unit,
    crossinline signal: (I, N, T, Any?) -> Unit
) = object : Context<N, T, I> {
    override val index: SimpleMap<I, Entity<N, T, I>>
        get() = index

    override fun newId() =
        newId()

    override fun releaseId(id: I) =
        releaseId(id)

    override fun signal(identity: I, name: N, time: T, args: Any?) =
        signal(identity, name, time, args)
}

/**
 * Base class for all entities, receives a context in which registration and dispatch is performed.
 * @property context The receiver context.
 * @param init The post-creation hook to run, deals with generation of identities.
 */
sealed class Entity<N, T : Comparable<T>, I : Comparable<I>>(
    val context: Context<N, T, I>,
    init: Entity<N, T, I>.() -> Unit
) {
    /**
     * The actual value of the identity, initially empty.
     */
    private var idValue: Option<I> = None

    init {
        // Run post init, `this` won't leak, as accessed properties are only base instance.
        @Suppress("leakingThis")
        init(this)
    }

    /**
     * The identity of the entity. Signalling will dispatch via this value.
     */
    var id: I
        get() = (idValue as? Just)?.item
            ?: throw IllegalStateException("Identity not assigned")
        set(value) {
            // Remove existing attachment.
            (idValue as? Just)?.item?.let {
                context.index.remove(it)
            }

            // Assign backing.
            idValue = Just(value)

            // Bind new attachment.
            context.index[value] = this
        }

    /**
     * Deletes this entity from the index and releases the ID.
     */
    fun delete() {
        // Delete is a semantic action, it has nothing to do with how IDs are assigned and values are restored.
        context.index.remove(id)
        context.releaseId(id)
    }

    /**
     * Sends a signal on self with the given name/time/argument triple.
     */
    fun signal(name: N, time: T, args: Any?) {
        context.signal(id, name, time, args)
    }

    /**
     * Locally evaluates the instruction for an undo.
     *
     * Default does nothing.
     */
    open fun evaluate(name: N, time: T, args: Any?): () -> Unit = { }
}

/**
 * Returns an entity relative name.
 */
fun <N, I : Comparable<I>> Entity<N, *, I>.name(name: N) =
    id to name

/**
 * Entity always generating an ID for itself. For entities that can be restored from a [Restore], use
 * [RestoringEntity], which skips ID generation if restoring.
 * @param context The receiver context.
 */
abstract class TrackingEntity<N, T : Comparable<T>, I : Comparable<I>>(
    context: Context<N, T, I>
) : Entity<N, T, I>(context, {
    id = context.newId()
})

/**
 * Entity that skips automatic ID generation if in restore mode. When only tracking and dispatch are
 * needed, use [TrackingEntity].
 * @param context The receiver context.
 * @param restore The restore context, given when this entity is reconstructed, rather then initialized.
 */
abstract class RestoringEntity<N, T : Comparable<T>, I : Comparable<I>>(
    context: Context<N, T, I>,
    restore: Restore?
) : Entity<N, T, I>(context, {
    if (restore == null)
        id = context.newId()
}) {
    /**
     * Collects the save-methods.
     */
    private val save = mutableListOf<(Store) -> Unit>()

    /**
     * Appends a save block that writes relevant data to the [Store]. This lambda [block] will be called in [save].
     */
    fun saveWith(block: (Store) -> Unit) {
        save.add(block)
    }

    /**
     * Saves this entity to the store.
     */
    fun save(store: Store) {
        save.forEach {
            it(store)
        }
    }
}

/**
 * Uses the entities accessible by the receiver to evaluate the incoming instruction, which is keyed by
 * the entity index, as well as the name of the instruction.
 */
fun <N, T : Comparable<T>, I : Comparable<I>> SimpleMap<I, Entity<N, T, I>>.dispatchEvaluate(
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
fun <N, T : Comparable<T>, I : Comparable<I>> entityMap() =
    map<I, Entity<N, T, I>>()