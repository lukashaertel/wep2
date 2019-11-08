package eu.metatools.up.aspects

import eu.metatools.up.dt.Instruction
import eu.metatools.up.dt.Lx
import eu.metatools.up.notify.Event

/**
 * Dispatches and receives instructions to an identified object.
 */
interface Dispatch : Aspect {
    /**
     * Listen to for receiving instructions on key.
     */
    val handleReceive: Event<Lx, Instruction>

    /**
     * Send an instruction as [id], counterpart of [handleReceive].
     */
    fun send(id: Lx, instruction: Instruction)
}