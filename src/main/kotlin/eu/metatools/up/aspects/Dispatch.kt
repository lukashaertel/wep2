package eu.metatools.up.aspects

import eu.metatools.up.dt.Instruction
import eu.metatools.up.dt.Lx
import eu.metatools.up.notify.Event
import eu.metatools.up.notify.EventList

/**
 * Dispatches and receives instructions to an identified object.
 */
interface Dispatch : Aspect {
    /**
     * Handles preparing for receiving an instruction.
     */
    val handlePrepare: Event<Lx, Instruction>

    /**
     * Listen to for receiving instructions on key.
     */
    val handlePerform: Event<Lx, Instruction>

    /**
     * Listen to for receiving end of perform.
     */
    val handleComplete: Event<Lx, Instruction>

    /**
     * Send an instruction as [id], counterpart of [handlePerform].
     */
    fun send(id: Lx, instruction: Instruction)
}