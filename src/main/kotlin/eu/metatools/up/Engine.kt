package eu.metatools.up

import eu.metatools.up.dt.Instruction
import eu.metatools.up.dt.Lx
import eu.metatools.up.dt.Time
import eu.metatools.up.notify.Callback
import eu.metatools.up.notify.Event

/**
 * Mode of the engine.
 */
enum class Mode {
    /**
     * In between actual operations.
     */
    Idle,
    /**
     * Restoring from fields.
     */
    RestoreData,

    /**
     * Storing to fields.
     */
    StoreData,

    /**
     * Revoking instructions.
     */
    Revoke,

    /**
     * Invoking instructions.
     */
    Invoke
}

/**
 * Engine implementing actual connection, translation, and administration.
 */
interface Engine : Shell {
    /**
     * Current mode.
     */
    val mode: Mode
    /**
     * Detached handler for saving to [save].
     */
    val onSave: Callback
    /**
     * Instruction execution limit.
     */
    var limit: Time
    /**
     * Detached handler for instructions on [Ent]s.
     */
    val onPerform: Event<Lx, Instruction>

    /**
     * Converts a value to a proxy.
     */
    fun toProxy(value: Any?): Any?

    /**
     * Converts a proxy to a value.
     */
    fun toValue(proxy: Any?): Any?

    /**
     * Loads a value from the field. Usually invoked on connecting an [Ent].
     */
    fun load(id: Lx): Any?

    /**
     * Saves a value to the field. Usually invoked from the [onSave] detached handlers.
     */
    fun save(id: Lx, value: Any?)

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
     * Includes an [Ent] in the table. Connects it.
     */
    fun add(ent: Ent)

    /**
     * Excludes an [Ent] from the table. Disconnects it.
     */
    fun remove(id: Lx)
}

/**
 * Converts the instruction to a proxy instruction via [Engine.toProxy].
 */
fun Instruction.toProxyWith(engine: Engine) =
    Instruction(target, methodName, time, args.map(engine::toProxy))

/**
 * Converts the instruction to a value instruction via [Engine.toValue].
 */
fun Instruction.toValueWith(engine: Engine) =
    Instruction(target, methodName, time, args.map(engine::toValue))

/**
 * Runs the local method with the var-arg list.
 */
fun Engine.local(vararg instructions: Instruction) =
    local(instructions.asSequence())