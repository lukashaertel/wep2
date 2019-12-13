package eu.metatools.up.deb

import eu.metatools.up.*
import eu.metatools.up.dt.Instruction
import java.lang.Appendable


data class LoggingDriver<D : Driver>(val on: D, val appendable: Appendable) : Driver {
    override val ent: Ent
        get() = on.ent
    override val isConnected: Boolean
        get() = on.isConnected

    override fun configure(part: Part) {
        appendable.appendln("driver configure $part")
        on.configure(part)
        appendable.appendln("end")
    }

    override fun connect(entIn: EntIn?) {
        appendable.appendln("driver connect $entIn")
        on.connect(entIn)
        appendable.appendln("end")
    }

    override fun persist(entOut: EntOut) {
        appendable.appendln("driver persist $entOut")
        on.persist(entOut)
        appendable.appendln("end")
    }

    override fun disconnect() {
        appendable.appendln("driver disconnect")
        on.disconnect()
        appendable.appendln("end")
    }

    override fun perform(instruction: Instruction) {
        appendable.appendln("driver perform $instruction")
        on.perform(instruction)
        appendable.appendln("end")
    }

    override fun ready() {
        appendable.appendln("driver ready")
        on.ready()
        appendable.appendln("end")
    }
}