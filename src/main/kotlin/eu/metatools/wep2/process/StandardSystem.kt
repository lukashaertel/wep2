package eu.metatools.wep2.process

import eu.metatools.wep2.coord.Warp
import eu.metatools.wep2.entity.*
import eu.metatools.wep2.entity.bind.*
import eu.metatools.wep2.tools.ReclaimableSequence
import eu.metatools.wep2.tools.ScopedSequence
import eu.metatools.wep2.tools.Time
import eu.metatools.wep2.tools.TimeGenerator
import eu.metatools.wep2.track.*
import eu.metatools.wep2.track.bind.prop
import eu.metatools.wep2.util.randomInts
import eu.metatools.wep2.util.shorts
import eu.metatools.wep2.util.within
import org.lwjgl.Sys

/**
 * Names generated and processed by a [StandardSystem].
 */
sealed class StandardName<N>

/**
 * Type for management messages.
 */
sealed class ManagementName<N> : StandardName<N>()

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
 * @property playerCount The player count of the system.
 * @property scopes The currently used scopes in the time generation process.
 * @property instructions The set of instructions.
 */
class StandardInitializer<N, P>(
    val idsHead: Short?,
    val idsRecycled: List<Pair<Short, Short>>,
    val randomSeed: Long,
    val randomsHead: Int?,
    val randomsRecycled: List<Pair<Int, Short>>,
    val playerCount: Short,
    val scopes: Map<Long, Byte>,
    val instructions: List<Triple<StandardName<N>, Time, Any?>>,
    val parameter: P,
    val saveData: Map<String, Any?>
)

/**
 * Creates a standard system, initializing or restoring all components from an optional [StandardInitializer].
 * @param createSeed The seed to use if creating fresh.
 * @param createParameters The parameter to use if creating fresh.
 * @param standardInitializer The initializer or null if creating.
 */
abstract class StandardSystem<N, P>(
    createSeed: Long,
    createParameters: P,
    standardInitializer: StandardInitializer<N, P>?
) : Warp<StandardName<N>, Time>(), Context<N, Time, SI> {

    companion object {
        /**
         * The starting ID.
         */
        val idZero = 0.toShort()

        /**
         * The recycler zero value for IDs.
         */
        val idRecycleZero = 0.toShort()

        /**
         * The recycler zero value for randoms.
         */
        val randomRecycleZero = 0.toShort()
    }

    /**
     * Amount of players.
     */
    var playerCount: Short = 1

    /**
     * Local player identity, should be unique if running across systems.
     */
    var self: Short = 0

    // TODO: Player management.

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
    override val ids = claimer(idReclaimableSequence)

    /**
     * The used random seed, if `standardInitializer` is passed, it is restored.
     */
    val randomSeed = standardInitializer?.randomSeed ?: createSeed

    /**
     * Generates the random values, if `standardInitializer` is passed, it is restored.
     */
    private val randomReclaimableSequence =
        standardInitializer?.let {
            // Restore from package.
            ReclaimableSequence.restore(
                randomInts(it.randomSeed), randomRecycleZero, Short::inc,
                it.randomsHead, it.randomsRecycled
            )
        } ?: ReclaimableSequence(randomInts(randomSeed), randomRecycleZero, Short::inc)

    /**
     * Claims random numbers.
     */
    val randoms = claimer(randomReclaimableSequence)

    /**
     * Generates the time values.
     */
    val time =
        standardInitializer?.let {
            TimeGenerator(it.playerCount, ScopedSequence.restore(TimeGenerator.defaultLocalIDs, it.scopes))
        } ?: TimeGenerator(playerCount)

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
            idReclaimableSequence.generatorHead,
            idReclaimableSequence.recycled,
            randomSeed,
            randomReclaimableSequence.generatorHead,
            randomReclaimableSequence.recycled,
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
        consolidate(time.take(upTo, Short.MIN_VALUE))
        time.consolidate(upTo)
    }

    private fun evaluateManagement(name: ManagementName<N>, time: Time, args: Any?): () -> Unit {
        // TODO: Management actions.
        return { -> }
    }

    override fun evaluate(name: StandardName<N>, time: Time, args: Any?) = when (name) {
        is ManagementName -> evaluateManagement(name, time, args)
        is ActiveName -> index.dispatchEvaluate(name.name, time, args)
    }

    /**
     * Takes a time for the current player ([self]) and the given [outer] time.
     */
    fun time(outer: Long) =
        time.take(outer, self)

    // TODO: Tick generators.
}

typealias StandardEntity<N> = RestoringEntity<N, Time, SI>

class X(
    createSeed: Long,
    createParameters: Int,
    standardInitializer: StandardInitializer<String, Int>?
) : StandardSystem<String, Int>(createSeed, createParameters, standardInitializer)

class E(
    context: X,
    restore: Restore?
) : StandardEntity<String>(context, restore) {
    // TODO: Nicer variant of this.
    val x get() = context as X

    var value by prop(restore) { 0 }
    var ar by prop(restore) { 0 }
    var br by prop(restore) { 0 }
    override fun evaluate(name: String, time: Time, args: Any?) =
        when (name) {
            "inc" -> rec { value++ }
            "dec" -> rec { value-- }
            "sar" -> rec { ar = x.randoms.claimValue().within(0, 100) }
            "sbr" -> rec { br = x.randoms.claimValue().within(0, 100) }
            else -> { -> }
        }

}

fun main() {
    val a = X(0L, 123, null)
    a.playerCount = 2
    a.self = 0

    val e = E(a, null)
    e.signal("inc", a.time(System.currentTimeMillis()), Unit)
    e.signal("inc", a.time(System.currentTimeMillis()), Unit)
    e.signal("inc", a.time(System.currentTimeMillis()), Unit)
    a.consolidate(System.currentTimeMillis())

    val b = X(0L, 432, a.save().also {
        println(it)
    })
    a.self = 1

    a.register(b::receive)
    b.register(a::receive)

    val f = b.index.mapNotNull { (_, e) -> e as E? }.first()
    f.signal("dec", b.time(System.currentTimeMillis()), Unit)

    e.signal("sar", a.time(System.currentTimeMillis()), Unit)
    f.signal("sbr", b.time(System.currentTimeMillis()), Unit)

    println(e)
}