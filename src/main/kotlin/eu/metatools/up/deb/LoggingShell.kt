package eu.metatools.up.deb

import eu.metatools.up.*
import eu.metatools.up.dt.Instruction
import eu.metatools.up.dt.Lx
import java.lang.Appendable
import kotlin.reflect.KClass

data class LoggingShell<S : Shell>(val on: S, val appendable: Appendable) : Shell {
    override val engine: Engine
        get() = LoggingEngine(this, on.engine, appendable)

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

    override fun load(shellIn: ShellIn) {
        appendable.appendln("shell load $shellIn")
        on.load(shellIn)
        appendable.appendln("end")
    }

    override fun store(shellOut: ShellOut) {
        appendable.appendln("shell store $shellOut")
        on.store(shellOut)
        appendable.appendln("end")
    }

    private var sendValue: ((Instruction) -> Unit)? = null

    override var send: ((Instruction) -> Unit)?
        get() = sendValue
        set(value) {
            sendValue = value

            if (value == null)
                on.send = null
            else
                on.send = {
                    appendable.appendln("shell send $it")
                    value(it)
                    appendable.appendln("end")
                }
        }

    override fun receive(instructions: Sequence<Instruction>) {
        appendable.appendln("shell receive ${instructions.joinToString(", ")}")
        on.receive(instructions)
        appendable.appendln("end")
    }
}