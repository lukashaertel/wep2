package eu.metatools.up

import eu.metatools.up.aspects.*
import eu.metatools.up.dt.*
import eu.metatools.up.lang.autoClosing
import eu.metatools.up.lang.constructBy
import eu.metatools.up.lang.label
import eu.metatools.up.lang.never
import eu.metatools.up.structure.Container
import eu.metatools.up.structure.Part
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSupertypeOf

/**
 * Unique ID of the system domain. Do not use this key as a root node.
 */
val systemDomain = (UUID.fromString("6aa03267-b187-415d-8b8f-2e93ae27cc1b") ?: never)
    .label("systemDomain")
/**
 * Primary entity table.
 */
val PET = lx / systemDomain / "PET"

abstract class Ent(on: Aspects?, override val id: Lx) : With(on), Container, Part {
    /**
     * Receiver connection, automatically closed on reassign.
     */
    private var closeReceive by autoClosing()

    /**
     * Store connection, automatically closed on reassign.
     */
    private var closeSave by autoClosing()

    /**
     * The parts of the entity.
     */
    private val parts = TreeMap<Lx, Part>()

    override fun resolve(id: Lx) =
        parts[id subtract this.id]

    override fun include(id: Lx, part: Part) {
        parts[id subtract this.id] = part
    }

    override fun connect() {
        // If dispatch is provided.
        this<Dispatch> {
            // Assign close.
            closeReceive = handleReceive.register(id) { perform(it) }
        }

        // Register saving if needed. Loading must be done externally, as existence is self dependent.
        this<Store> {
            // Register storing the entity to the primary entity table.
            closeSave = handleSave.register {
                // Get the arguments or use empty to signal default construction rules should be applied.
                val args = with<Args>()?.extraArgs.orEmpty()

                // Store to PET under the entities own identity.
                it(PET / id, this@Ent::class to args)
            }
        }

        // Connect in ascending order.
        parts.forEach { (_, part) ->
            part.connect()
        }
    }

    override fun disconnect() {
        // Stop in descending order.
        parts.descendingMap().forEach { (_, part) ->
            part.disconnect()
        }

        // Reset close, will close open connection via delegate.
        closeReceive = null
    }

    protected open fun perform(instruction: Instruction) {
        throw IllegalArgumentException("Unknown instruction $instruction")
    }

    fun send(instruction: Instruction) {
        this<Dispatch> {
            send(id, instruction)
        }
    }
}

/**
 * Reconstructs all [Ent]s that are stored in the receivers [Store] aspects, constructs them on the receiver.
 */
inline fun Aspects.reconstructPET(receive: (Lx, Ent) -> Unit) {
    this<Store> {
        for (petEntry in lsr(PET)) {
            // Load class and data.
            @Suppress("unchecked_cast")
            val data = load(petEntry) as Pair<KClass<out Ent>, Map<String, Any?>>

            // Get the ID as relative to the PET.
            val id = petEntry subtract PET

            // Receive new entity, constructed by the data and the receiver aspects.
            receive(id subtract PET, data.first.constructBy(data.second) {
                when {
                    // If type is the aspects receiver, return the aspects passed to the function.
                    it.type.isSupertypeOf(Aspects::class.createType(nullable = true)) ->
                        Box(this@reconstructPET)

                    // If type is identity receiver, return the given id.
                    it.type.isSupertypeOf(Lx::class.createType(nullable = false)) ->
                        Box(id)

                    // Other parameters should not be assigned.
                    else ->
                        null
                }
            })
        }
    }
}