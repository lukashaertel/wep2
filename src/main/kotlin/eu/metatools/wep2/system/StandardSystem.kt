package eu.metatools.wep2.system

import eu.metatools.wep2.aspects.Resolving
import eu.metatools.wep2.aspects.Restoring
import eu.metatools.wep2.aspects.Saving
import eu.metatools.wep2.components.*
import eu.metatools.wep2.coordinators.Warp
import eu.metatools.wep2.entity.*
import eu.metatools.wep2.storage.*
import eu.metatools.wep2.system.StandardSystem.Companion.create
import eu.metatools.wep2.tools.*
import eu.metatools.wep2.system.StandardSystem.Companion.playerGaia
import eu.metatools.wep2.track.rec
import eu.metatools.wep2.util.ComparablePair
import eu.metatools.wep2.util.collections.SimpleMap
import eu.metatools.wep2.util.listeners.Listener
import eu.metatools.wep2.util.shorts
import java.io.Serializable
import java.util.*
import kotlin.NoSuchElementException
import kotlin.collections.getValue
import kotlin.collections.set

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
 *
 * @property restore The source of the restore operation.
 */
class StandardSystem2<N, P>(n
    override val restore: Restore?,
    val toSystemTime: (Long) -> Long,
    parameter: () -> P
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
     * Collects the save-methods.
     */
    private val save = mutableListOf({ _: Store ->
        // Before all saves, undo all operations in the instruction cache.
        undoAll()
    })

    /**
     * Adds the [block] to the backing.
     */
    override fun saveWith(block: (Store) -> Unit) {
        save.add(block)
    }

    /**
     * Players are a claimer, starting at the zero values defined in the companion.
     */
    private val players by claimer(shorts(playerZero), playerRecycleZero, Short::inc)

    /**
     * The local player, might be restored.
     */
    var playerSelf by prop { players.claim() }
        private set

    /**
     * The player count, starts at one.
     */
    var playerCount by prop { 1.toShort() }
        private set

    /**
     * The local player name.
     */
    val self = playerSelf.first

    /**
     * The IDs, as needed by the context to claim new entity identities.
     */
    private val ids by claimer(shorts(idZero), idRecycleZero, Short::inc)

    /**
     * The local time generator, might be restored from occupied scopes.
     */
    private val time by timer()

    /**
     * The initialization parameter as given, or as restored.
     */
    val parameter by prop(Listener.EMPTY, parameter, false)

    /**
     * The serial self value, used for loop-back disambiguation.
     */
    private val serialSelf = UUID.randomUUID()

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
            receiveAll(restore.load(key))
        },
        { store, key ->
            store.save(key, instructions.toList())
        })

    /**
     * The central entity index.
     */
    override val index by entityMap<N, Time, SI>()

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

    /**
     * Signals a non-management name to the system.
     */
    override fun signal(identity: SI, name: N, time: Time, args: Any?) {
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

    /**
     * Post save will redo all instructions, as the first save operation is to undo everything.
     */
    init {
        saveWith {
            redoAll()
        }
    }
}

/**
 * A standard system, initializing or restoring all components from an optional [StandardInitializer].
 *
 * Create a new instance with [create].
 * @property parameters The parameter to use if creating fresh.
 * @property standardInitializer The initializer or null if creating.
 * @property toSystemTime The conversion from external to system time.
 */
class StandardSystem<N, P> private constructor(
    val parameters: P,
    val standardInitializer: StandardInitializer<N, P>?,
    val toSystemTime: (Long) -> Long
) : Warp<StandardName<N>, Time>(), StandardContext<N> {
    /**
     * True if the system was restored.
     */
    val wasRestored = standardInitializer != null

    /**
     * A value that can be exchanged but marks local referral.
     */
    private val serialSelf = UUID.randomUUID()

    companion object {
        /**
         * Creates or restores a standard system with optional initializer argument.
         *
         * @param parameters The parameter to use if creating fresh.
         * @param standardInitializer The initializer or null if creating.
         * @param toSystemTime The conversion from external to system time.
         */
        fun <N, P> create(
            parameters: P,
            standardInitializer: StandardInitializer<N, P>?,
            toSystemTime: (Long) -> Long = { it }
        ) = StandardSystem(parameters, standardInitializer, toSystemTime).apply {
            // If initializer is given, restore from it and receive all instructions.
            standardInitializer?.let {
                // Restore all entities from the saved data.
                restoreBy(it.saveData::getValue) { restore ->
                    restoreIndex(this, restore)
                }

                // Receive all instructions.
                receiveAll(it.instructions.asSequence())
            }
        }

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
     * Generates the player numbers, if `standardInitializer` is passed, it is restored.
     */
    private val playerReclaimableSequence =
        standardInitializer?.let {
            // Restore from package.
            ReclaimableSequence.restore(
                shorts(playerZero), playerRecycleZero, Short::inc,
                it.playerHead, it.playerRecycled
            )
        } ?: ReclaimableSequence(shorts(playerZero), playerRecycleZero, Short::inc)

    /**
     * Claims player numbers.
     */
    private val players = Claimer(playerReclaimableSequence)

    /**
     * The current player with recycle count.
     */
    var playerSelf by prop { standardInitializer?.playerSelf ?: players.claim() }
        private set

    /**
     * The current player count.
     */
    var playerCount by prop { standardInitializer?.playerCount ?: 1 }
        private set

    /**
     * The current player number, claim new with [claimNewPlayer].
     */
    val self get() = playerSelf.first

    /**
     * Generates the identities, if `standardInitializer` is passed, it is restored.
     */
    private val idReclaimableSequence =
        standardInitializer?.let {
            // Restore from package.
            ReclaimableSequence.restore(
                shorts(idZero), idRecycleZero, Short::inc,
                it.idsHead, it.idsRecycled
            )
        } ?: ReclaimableSequence(shorts(idZero), idRecycleZero, Short::inc)

    /**
     * Claims identities for entities.
     */
    val ids = Claimer(idReclaimableSequence)

    /**
     * Generates the time values.
     */
    val time =
        standardInitializer?.let {
            TimeGenerator(ScopedSequence.restore(TimeGenerator.defaultLocalIDs, it.scopes))
        } ?: TimeGenerator(ScopedSequence(TimeGenerator.defaultLocalIDs))

    /**
     * Central entity index.
     */
    override val index by entityMap<N, Time, SI>()

    /**
     * The used parameter, if `standardInitializer` is passed, it is restored.
     */
    var parameter = standardInitializer?.parameter ?: parameters

    override fun newId() =
        ids.claim()

    override fun releaseId(id: SI) =
        ids.release(id)

    override fun signal(identity: SI, name: N, time: Time, args: Any?) {
        signal(ActiveName(identity to name), time, args)
    }

    /**
     * Undoes all instructions and creates a restore package for this state, then redoes the instructions.
     */
    fun save(): StandardInitializer<N, P> {
        undoAll()

        // Create save data.
        val saveData = mutableMapOf<String, Any?>()

        // Store entity values.
        storeBy(saveData::set) { store ->
            storeIndex(this, store)
        }

        val result = StandardInitializer(
            playerReclaimableSequence.generatorHead,
            playerReclaimableSequence.recycled,
            idReclaimableSequence.generatorHead,
            idReclaimableSequence.recycled,
            playerSelf,
            playerCount,
            time.localIDs.scopes,
            instructions,
            parameter,
            saveData
        )

        redoAll()

        return result
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

    override fun evaluate(name: StandardName<N>, time: Time, args: Any?) = when (name) {
        is ManagementName -> evaluateManagement(name, time, args)
        is ActiveName -> index.dispatchEvaluate(name.name, time, args)
    }

    /**
     * Takes a time for the current player ([self]) and the given [generalTime] time. If no
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
inline fun <N, reified R> StandardSystem<N, *>.single() =
    index.values.asSequence().filterIsInstance<R>().single()

/**
 * Finds the first matching entity or throws an exception.
 */
inline fun <N, reified R> StandardSystem<N, *>.find() =
    index.values.asSequence().filterIsInstance<R>().first()

/**
 * Finds the first matching entity or returns null.
 */
inline fun <N, reified R> StandardSystem<N, *>.firstOrNull() =
    index.values.asSequence().filterIsInstance<R>().firstOrNull()

/**
 * Generates ticks to the element identified by [name] at the given [time]. The coordinator, player
 * count and time generator are taken from the [standardSystem].
 */
fun <N> TickGenerator.tickToWith(standardSystem: StandardSystem<N, *>, name: SN<N>, time: Long) = tickToWith(
    standardSystem.time, standardSystem,
    ActiveName(name),
    standardSystem.toSystemTime(time),
    standardSystem.playerCount, StandardSystem.playerGaia
)