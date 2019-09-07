package eu.metatools.wep2.system

import eu.metatools.wep2.coord.Warp
import eu.metatools.wep2.entity.*
import eu.metatools.wep2.entity.bind.restoreBy
import eu.metatools.wep2.entity.bind.restoreIndex
import eu.metatools.wep2.entity.bind.storeBy
import eu.metatools.wep2.entity.bind.storeIndex
import eu.metatools.wep2.tools.*
import eu.metatools.wep2.track.Claimer
import eu.metatools.wep2.track.SI
import eu.metatools.wep2.track.prop
import eu.metatools.wep2.track.rec
import eu.metatools.wep2.util.shorts
import java.util.*
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.asSequence
import kotlin.collections.filterIsInstance
import kotlin.collections.getValue
import kotlin.collections.mutableMapOf
import kotlin.collections.set
import kotlin.collections.single

/**
 * Names generated and processed by a [StandardSystem].
 */
sealed class StandardName<in N>

/**
 * Type for management messages.
 */
sealed class ManagementName<N> : StandardName<N>()

/**
 * Claim player.
 */
object ClaimPlayer : ManagementName<Any?>()

/**
 * Release player
 */
object ReleasePlayer : ManagementName<Any?>()

/**
 * Type for actual messages.
 */
data class ActiveName<N>(val name: SN<N>) : StandardName<N>()

/**
 * A standard initializer.
 * @property idsHead The current head of the ID generation process.
 * @property idsRecycled The currently recycled IDs.
 * @property randomSeed The seed of the random.
 * @property randomsHead The head of the random generation process.
 * @property idsRecycled The currently recycled randoms.
 *
 * @property scopes The currently used scopes in the time generation process.
 * @property instructions The set of instructions.
 */
class StandardInitializer<N, P>(
    val playerHead: Short?,
    val playerRecycled: List<Pair<Short, Short>>,
    val idsHead: Short?,
    val idsRecycled: List<Pair<Short, Short>>,
    val playerSelf: Pair<Short, Short>,
    val playerCount: Short,
    val scopes: Map<Long, Byte>,
    val instructions: List<Triple<StandardName<N>, Time, Any?>>,
    val parameter: P,
    val saveData: Map<String, Any?>
)

// TODO: Restored times after a load will greatly differ, save. Maybe save and restore a delta?
//  Probably to be done in the frontend, creating a difference based time.


/**
 * A context for a [StandardSystem].
 */
typealias StandardContext<N> = Context<N, Time, SI>

/**
 * Creates a standard system, initializing or restoring all components from an optional [StandardInitializer].
 * @param createSeed The seed to use if creating fresh.
 * @param createParameters The parameter to use if creating fresh.
 * @param standardInitializer The initializer or null if creating.
 */
open class StandardSystem<N, P>(
    createSeed: Long,
    createParameters: P,
    standardInitializer: StandardInitializer<N, P>?
) : Warp<StandardName<N>, Time>(), StandardContext<N> {
    /**
     * A value that can be exchanged but marks local referral.
     */
    private val serialSelf = UUID.randomUUID()

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
    var playerSelf by prop(standardInitializer?.playerSelf ?: players.claim())
        private set

    /**
     * The current player count.
     */
    var playerCount by prop(standardInitializer?.playerCount ?: 1)
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
    override val index = entityMap<N, Time, SI>()

    /**
     * The used parameter, if `standardInitializer` is passed, it is restored.
     */
    var parameter = standardInitializer?.parameter ?: createParameters

    init {
        standardInitializer?.let {
            // Restore all entities from the saved data.
            restoreBy(it.saveData::getValue) { restore ->
                restoreIndex(this, restore)
            }

            // Receive all instructions.
            receiveAll(it.instructions.asSequence())
        }
    }

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
    fun consolidate(upTo: Long) {
        consolidate(time.take(upTo, playerCount, Short.MIN_VALUE))
        time.consolidate(upTo)
    }

    /**
     * Sends a claim player signal for [playerGaia] and on evaluation, assigns the value.
     */
    fun claimNewPlayer(generalTime: Long) {
        signal(ClaimPlayer, time.take(generalTime, playerCount, playerGaia), serialSelf)
    }

    /**
     * Releases the current player number.
     */
    fun releasePlayer(generalTime: Long) {
        signal(ReleasePlayer, time.take(generalTime, playerCount, playerGaia), playerSelf)
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
            args as Pair<Short, Short>

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
    fun time(generalTime: Long = System.currentTimeMillis()) =
        time.take(generalTime, playerCount, self)
}

/**
 * A [RestoringEntity] with the remaining parameters bound to match [StandardSystem].
 */
typealias StandardEntity<N> = RestoringEntity<N, Time, SI>

/**
 * Finds the single root entity or throws an exception.
 */
inline fun <N, reified R> StandardSystem<N, *>.findRoot() =
    index.values.filterIsInstance<R>().single()

/**
 * Generates ticks to the element identified by [name] at the given [time]. The coordinator, player
 * count and time generator are taken from the [standardSystem].
 */
fun <N> TickGenerator.tickToWith(standardSystem: StandardSystem<N, *>, name: SN<N>, time: Long) = tickToWith(
    standardSystem.time, standardSystem,
    ActiveName(name),
    time,
    standardSystem.playerCount, StandardSystem.playerGaia
)