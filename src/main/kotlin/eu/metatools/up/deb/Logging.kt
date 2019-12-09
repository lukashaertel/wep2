package eu.metatools.up.deb

import eu.metatools.up.*
import eu.metatools.up.dt.Instruction
import eu.metatools.up.dt.Lx
import eu.metatools.up.dt.Time
import eu.metatools.up.notify.Callback
import eu.metatools.up.notify.Event
import java.lang.Appendable
import kotlin.reflect.KClass

class LogShell<S : Shell>(val on: S, val output: Appendable = System.out) : Shell {
    override val engine = object : Engine {
        override val shell: Shell
            get() = this@LogShell

        override val isLoading: Boolean
            get() = on.engine.isLoading

        private val ld
            get() = if (isLoading) "LD " else ""

        override fun amend(driver: Driver): Driver {
            return LogDriver(on.engine.amend(driver), output)
        }

        override fun load(id: Lx): Any? {
            return on.engine.load(id).also {
                output.appendln("$ld LOAD $id VALUE $it")
            }
        }

        override val onSave: Callback
            get() = on.engine.onSave

        override fun save(id: Lx, value: Any?) {
            return on.engine.save(id, value).also {
                output.appendln("$ld SAVE $id VALUE $value")
            }
        }

        override val onAdd: Event<Lx, Ent>
            get() = on.engine.onAdd

        override fun add(ent: Ent) {
            return on.engine.add(ent).also {
                output.appendln("$ld ADD ${ent.id} ENT $ent")
            }
        }

        override val onRemove: Event<Lx, Ent>
            get() = on.engine.onRemove

        override fun remove(ent: Ent) {
            return on.engine.remove(ent).also {
                output.appendln("$ld REMOVE ${ent.id} ENT $ent")
            }
        }

        override fun capture(id: Lx, undo: () -> Unit) {
            on.engine.capture(id, undo)
        }

        override fun exchange(instruction: Instruction) {
            return on.engine.exchange(instruction).also {
                output.appendln("$ld EXCHANGE $instruction")
            }
        }

        override fun local(instructions: Sequence<Instruction>) {
            return on.engine.local(instructions).also {
                for (i in instructions)
                    output.appendln("$ld LOCAL $i")
            }
        }

        override fun invalidate(global: Long) {
            return on.engine.invalidate(global).also {
                output.appendln("$ld INVALIDATE $global")
            }
        }
    }

    override val initializedTime: Long
        get() = on.initializedTime

    override val player: Short
        get() = on.player

    override fun time(global: Long) =
        on.time(global)

    override fun resolve(id: Lx) =
        on.resolve(id)

    override fun <T : Any> list(kClass: KClass<T>) =
        on.list(kClass)

}

class LogDriver<D : Driver>(val on: D, val output: Appendable = System.out) : Driver {
    override val ent: Ent
        get() = on.ent

    override val isConnected: Boolean
        get() = on.isConnected

    override fun include(id: Lx, part: Part) {
        on.include(id, part).also {
            output.appendln("${ent.id} INCLUDE $id PART $part")
        }
    }

    override fun connect() {
        on.connect().also {
            output.appendln("${ent.id} CONNECT")
        }
    }

    override fun disconnect() {
        on.disconnect().also {
            output.appendln("${ent.id} DISCONNECT")
        }
    }

    override fun perform(instruction: Instruction) {
        on.perform(instruction).also {
            output.appendln("${ent.id} PERFORM $instruction")
        }
    }

}