package eu.metatools.wep2.nes.aspects

import eu.metatools.wep2.nes.dt.Instruction
import eu.metatools.wep2.nes.dt.Lx
import eu.metatools.wep2.nes.notify.Event

interface Dispatch : Aspect {
    /**
     * Listen to for receiving instructions on key.
     */
    val receive: Event<Lx, Instruction>

    /**
     * Send an instruction as [id], counterpart of [receive].
     */
    fun send(id: Lx, instruction: Instruction)
}