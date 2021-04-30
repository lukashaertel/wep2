package eu.metatools.up

import eu.metatools.up.dt.Instruction

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
     * Includes an [Ent] in the table. Connects it.
     */
    fun add(ent: Ent)

    /**
     * Excludes an [Ent] from the table. Disconnects it.
     */
    fun remove(ent: Ent)

    /**
     * True if capturing at the moment.
     */
    val isCapturing: Boolean

    /**
     * Captures an undo. Captures nothing if not [isCapturing].
     */
    fun capture(undo: () -> Unit): Unit =
        throw UnsupportedOperationException("This engine does not support undo.")

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
    fun invalidate(global: Long) {
        // Do nothing.
    }

    fun lastRepeatingTime(player: Short, rdc: Byte): Long? =
        throw UnsupportedOperationException("This engine does not support repeating.")
}


/**
 * Runs the local method with the var-arg list.
 */
fun Engine.local(vararg instructions: Instruction) =
    local(instructions.asSequence())