package eu.metatools.wep2.nes

import eu.metatools.wep2.nes.aspects.*
import eu.metatools.wep2.nes.dt.Instruction
import eu.metatools.wep2.nes.dt.Lx
import eu.metatools.wep2.nes.dt.div
import eu.metatools.wep2.nes.dt.subtract
import eu.metatools.wep2.nes.lang.autoClosing
import eu.metatools.wep2.nes.lang.constructBy
import eu.metatools.wep2.nes.structure.Container
import eu.metatools.wep2.nes.structure.Part
import java.util.*
import kotlin.reflect.KClass

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
            closeReceive = receive.register(id) { perform(it) }
        }

        // Register saving if needed. Loading must be done externally, as existence is self dependent.
        this<Store> {
            // Register storing the entity to the primary entity table.
            closeSave = save.register {
                // Get the arguments or use empty to signal default construction rules should be applied.
                val args = with<Construct>()?.constructorArgs.orEmpty()

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

inline fun Store.reconstructPET(receive: (Lx, Ent) -> Unit) {
    for (id in lsr(PET)) {
        // Load class and data.
        @Suppress("unchecked_cast")
        val data = load(id) as Pair<KClass<out Ent>, Map<String, Any?>>
        receive(id, data.first.constructBy(data.second))
    }
}