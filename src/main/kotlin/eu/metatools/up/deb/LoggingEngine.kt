package eu.metatools.up.deb

import eu.metatools.up.Driver
import eu.metatools.up.Engine
import eu.metatools.up.Ent
import eu.metatools.up.Shell
import eu.metatools.up.dt.Instruction
import eu.metatools.up.dt.Time

data class LoggingEngine<S : Shell, E : Engine>(override val shell: S, val on: E, val appendable: Appendable) : Engine {

    override fun amend(driver: Driver): Driver {
        return LoggingDriver(on.amend(driver), appendable)
    }

    override fun add(ent: Ent) {
        appendable.appendln("engine add $ent")
        on.add(ent)
        appendable.appendln("end")
    }

    override fun remove(ent: Ent) {
        appendable.appendln("engine remove $ent")
        on.remove(ent)
        appendable.appendln("end")
    }

    override fun exchange(instruction: Instruction) {
        appendable.appendln("engine exchange $instruction")
        on.exchange(instruction)
        appendable.appendln("end")
    }

    override fun local(instructions: Sequence<Instruction>) {
        appendable.appendln("engine local ${instructions.joinToString(", ")}")
        on.local(instructions)
        appendable.appendln("end")
    }

    override fun capture(undo: () -> Unit) {
        on.capture(undo)
    }

    override fun invalidate(global: Long) {
        appendable.appendln("engine invalidate $global")
        on.invalidate(global)
        appendable.appendln("end")
    }
}