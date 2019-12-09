package eu.metatools.up

import eu.metatools.up.dt.Instruction
import eu.metatools.up.dt.Lx
import eu.metatools.up.dt.Time
import eu.metatools.up.notify.Callback
import eu.metatools.up.notify.Event
import java.lang.UnsupportedOperationException

/**
 * Engine implementing actual connection, translation, and administration.
 */
interface Engine {
    /**
     * Containing shell.
     */
    val shell: Shell

    /**
     * Wraps the driver used for running entities.
     */
    fun amend(driver: Driver): Driver =
        driver

    /**
     * True if initialization on [Part.connect] should load rather than initialize.
     */
    val isLoading: Boolean

    /**
     * Loads a value from the field. Usually invoked on connecting an [Ent].
     */
    fun load(id: Lx): Any?

    /**
     * Detached handler for saving to [save].
     */
    val onSave: Callback

    /**
     * Saves a value to the field. Usually invoked from the [onSave] detached handlers.
     */
    fun save(id: Lx, value: Any?)

    /**
     * On add of entity.
     */
    val onAdd: Event<Lx, Ent>
    /**
     * Includes an [Ent] in the table. Connects it.
     */
    fun add(ent: Ent)

    /**
     * On remove of entity.
     */
    val onRemove: Event<Lx, Ent>

    /**
     * Excludes an [Ent] from the table. Disconnects it.
     */
    fun remove(ent: Ent)

    /**
     * Captures the reset of the distinct [id] by invoking [undo]. Relative undo, should expect the post-state.
     */
    fun capture(id: Lx, undo: () -> Unit)

    /**
     * Performs the operation, invoke via the [Ent.exchange] wrappers.
     */
    fun exchange(instruction: Instruction)

    /**
     * Performs the operations locally. For [Instruction]s that are manually synchronized.
     */
    fun local(instructions: Sequence<Instruction>)

    /**
     * Invalidates every time constrained value before the given time.
     */
    fun invalidate(global: Long): Unit =
        throw UnsupportedOperationException()
}

/**
 * Runs the local method with the var-arg list.
 */
fun Engine.local(vararg instructions: Instruction) =
    local(instructions.asSequence())