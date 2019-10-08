package eu.metatools.wep2.system

import eu.metatools.wep2.aspects.Restoring
import eu.metatools.wep2.aspects.Saving
import eu.metatools.wep2.components.claimer
import eu.metatools.wep2.components.custom
import eu.metatools.wep2.components.prop
import eu.metatools.wep2.components.timer
import eu.metatools.wep2.coordinators.Warp
import eu.metatools.wep2.entity.*
import eu.metatools.wep2.storage.Restore
import eu.metatools.wep2.storage.Store
import eu.metatools.wep2.storage.path
import eu.metatools.wep2.tools.TickGenerator
import eu.metatools.wep2.tools.Time
import eu.metatools.wep2.tools.tickToWith
import eu.metatools.wep2.track.rec
import eu.metatools.wep2.util.ComparablePair
import eu.metatools.wep2.util.collections.ObservableMap
import eu.metatools.wep2.util.collections.ObservableMapListener
import eu.metatools.wep2.util.listeners.Listener
import eu.metatools.wep2.util.listeners.MapListener
import eu.metatools.wep2.util.shorts
import java.io.Serializable
import java.util.*

/**
 * Names generated and processed by a [StandardSystem].
 */
sealed class StandardName<in N> : Serializable

/**
 * Type for management messages.
 */
sealed class ManagementName<N> : StandardName<N>(), Serializable

/**
 * Claim player.
 */
object ClaimPlayer : ManagementName<Any?>(), Serializable {
    private fun readResolve(): Any? = ClaimPlayer
}

/**
 * Release player
 */
object ReleasePlayer : ManagementName<Any?>(), Serializable {
    private fun readResolve(): Any? = ReleasePlayer
}

/**
 * Type for actual messages.
 */
data class ActiveName<N>(val name: SN<N>) : StandardName<N>(), Serializable

// TODO: Restored times after a load will greatly differ, save. Maybe save and restore a delta?
//  Probably to be done in the frontend, creating a difference based time.


/**
 * A context for a [StandardSystem].
 */
typealias StandardContext<N> = Context<N, Time, SI>

/**
 * An instruction of a standard system.
 */
typealias StandardInstruction<N> = Triple<StandardName<N>, Time, Any?>

/**
 * System concurrency.
 */
enum class Concurrency {
    /**
     * No locking, all operations applied on the system from outside are manually synchronized.
     */
    UNLOCKED,

    /**
     * Synchronized on signalling and receiving.
     */
    SYNC
}

/**
 *
 * @property restore The source of the restore operation.
 */
class StandardSystem<N>(
    override val restore: Restore?,
    val toSystemTime: (Long) -> Long,
    val concurrency: Concurrency = Concurrency.UNLOCKED,
    playerSelfListener: Listener<Unit, ComparablePair<Short, Short>> = Listener.EMPTY,
    playerCountListener: Listener<Unit, Short> = Listener.EMPTY,
    indexListener: ObservableMapListener<SI, Entity<N, Time, SI>> = MapListener.EMPTY
) : Warp<StandardName<N>, Time>(), StandardContext<N>, Restoring, Saving {
    companion object {
        /**
         * The player used for ticks etc.
         */
        const val playerGaia = (-1).toShort()

        /**
         * The starting player number.
         */
        const val playerZero = 0.toShort()

        /**
         * The recycler zero value for player numbers.
         */
        const val playerRecycleZero = 0.toShort()

        /**
         * The starting ID.
         */
        const val idZero = 0.toShort()

        /**
         * The recycler zero value for IDs.
         */
        const val idRecycleZero = 0.toShort()
    }

    /**
     * Applies synchronization if needed.
     */
    private inline fun <R> applySync(block: () -> R) =
        if (concurrency == Concurrency.SYNC)
            synchronized(this, block)
        else
            run(block)

    /**
     * Collects the save-methods.
     */
    private val save = mutableListOf<(Store) -> Unit>()

    /**
     * Adds the [block] to the backing.
     */
    override fun saveWith(block: (Store) -> Unit) {
        save.add(block)
    }

    /**
     * Performs all nested saves, undoes and redoes all operations as necessary.
     */
    override fun save(store: Store) = applySync {
        // Before all saves, undo all operations in the instruction cache.
        undoAll()

        // Apply all nested saves.
        save.forEach {
            it(store)
        }

        // After save, redo all operations.
        redoAll()
    }


    /**
     * Players are a claimer, starting at the zero values defined in the companion.
     */
    private val players by claimer(shorts(playerZero), playerRecycleZero, Short::inc)

    /**
     * The local player, might be restored.
     */
    var playerSelf by prop(playerSelfListener) { players.claim() }
        private set

    /**
     * The player count, starts at one.
     */
    var playerCount by prop(playerCountListener) { 1.toShort() }
        private set

    /**
     * The local player name.
     */
    val self get() = playerSelf.first

    /**
     * The IDs, as needed by the context to claim new entity identities.
     */
    private val ids by claimer(shorts(idZero), idRecycleZero, Short::inc)

    /**
     * The local time generator, might be restored from occupied scopes.
     */
    val time by timer()

    /**
     * The serial self value, used for loop-back disambiguation.
     */
    private val serialSelf = UUID.randomUUID()

    /**
     * The central entity index.
     */
    override val index = ObservableMap(indexListener)

    /**
     * The index as a custom slot, responsible for restoring the index, and storing it.
     */
    private val indexSlot by custom(
        { restore, key ->
            restoreIndex(this, restore.path(key))
        },
        { store, key ->
            storeIndex(this, store.path(key))
        })

    /**
     * The instructions as a custom slot, will receive all on loading, and save all when storing.
     */
    private val instructionsSlot by custom(
        { restore, key ->
            // Load list of instructions.
            val instructions = restore.load<List<StandardInstruction<N>>>(key)

            // Receive them as a sequence.
            receiveAll(instructions.asSequence())
        },
        { store, key ->
            // Save instructions as a list.
            store.save(key, instructions.toList())
        })

    /**
     * Claims a new ID of the [ids].
     */
    override fun newId() =
        ids.claim()

    /**
     * Releases an ID to the [ids].
     */
    override fun releaseId(id: SI) =
        ids.release(id)

    override fun receive(name: StandardName<N>, time: Time, args: Any?) = applySync {
        super.receive(name, time, args)
    }

    override fun receiveAll(triples: Sequence<Triple<StandardName<N>, Time, Any?>>) = applySync {
        super.receiveAll(triples)
    }

    /**
     * Signals a non-management name to the system.
     */
    override fun signal(identity: SI, name: N, time: Time, args: Any?) = applySync {
        signal(ActiveName(identity to name), time, args)
    }

    /**
     * Evaluates the name, on non-management, an index dispatch will perform the operation.
     */
    override fun evaluate(name: StandardName<N>, time: Time, args: Any?) = when (name) {
        is ManagementName -> evaluateManagement(name, time, args)
        is ActiveName -> index.dispatchEvaluate(name.name, time, args)
    }

    /**
     * Consolidates the coordinator and the time generator.
     */
    fun consolidate(local: Long) {
        val actual = toSystemTime(local)
        consolidate(time.take(actual, playerCount, Short.MIN_VALUE))
        time.consolidate(actual)
    }

    /**
     * Sends a claim player signal for [playerGaia] and on evaluation, assigns the value.
     */
    fun claimNewPlayer(local: Long) {
        val actual = toSystemTime(local)
        signal(ClaimPlayer, time.take(actual, playerCount, playerGaia), serialSelf)
    }

    /**
     * Releases the current player number.
     */
    fun releasePlayer(local: Long) {
        val actual = toSystemTime(local)
        signal(ReleasePlayer, time.take(actual, playerCount, playerGaia), playerSelf)
    }

    /**
     * Evaluates the management name.
     */
    private fun evaluateManagement(name: ManagementName<N>, time: Time, args: Any?) = when (name) {
        ClaimPlayer -> rec {
            // Assert type of args.
            args as UUID

            // Claim new player and increment player count.
            val next = players.claim()
            playerCount++

            // If claiming for this local player, assign self.
            if (args == serialSelf)
                playerSelf = next
        }
        ReleasePlayer -> rec {
            // Assert type of args.
            @Suppress("unchecked_cast")
            args as ComparablePair<Short, Short>

            // Release player and decrement player count.
            players.release(args)
            playerCount--
        }
    }


    /**
     * Takes a time for the current player ([self]) and the given [local] time. If no
     * general time is given, uses the current time millis.
     */
    fun time(local: Long = System.currentTimeMillis()) =
        time.take(toSystemTime(local), playerCount, self)
}

/**
 * A [RestoringEntity] with the remaining parameters bound to match [StandardSystem].
 */
typealias StandardEntity<N> = RestoringEntity<N, Time, SI>

/**
 * Finds the single matching entity or throws an exception.
 */
inline fun <N, reified R> StandardSystem<N>.single() =
    index.values.asSequence().filterIsInstance<R>().single()

/**
 * Finds the first matching entity or throws an exception.
 */
inline fun <N, reified R> StandardSystem<N>.find() =
    index.values.asSequence().filterIsInstance<R>().first()

/**
 * Finds the first matching entity or returns null.
 */
inline fun <N, reified R> StandardSystem<N>.firstOrNull() =
    index.values.asSequence().filterIsInstance<R>().firstOrNull()

/**
 * Generates ticks to the element identified by [name] at the given [time]. The coordinator, player
 * count and time generator are taken from the [standardSystem].
 */
fun <N> TickGenerator.tickToWith(standardSystem: StandardSystem<N>, name: SN<N>, time: Long) = tickToWith(
    standardSystem.time, standardSystem,
    ActiveName(name),
    standardSystem.toSystemTime(time),
    standardSystem.playerCount, StandardSystem.playerGaia
)