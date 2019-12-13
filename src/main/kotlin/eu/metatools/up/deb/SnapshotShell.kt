package eu.metatools.up.deb

import eu.metatools.up.*
import eu.metatools.up.dt.Instruction
import eu.metatools.up.dt.Lx
import eu.metatools.up.dt.Time
import java.util.*
import kotlin.reflect.KClass

data class SnapshotShell<S : Shell>(
    val on: S,
    val frequency: Long,
    val receiver: Shell.(Long) -> Unit,
    val invalidated: Shell.(Long) -> Unit
) : Shell {
    private var lastSnapshotSlot: Long? = null
    private val sent = TreeSet<Long>()

    override val engine = object : Engine {
        override val shell: Shell
            get() = this@SnapshotShell

        override fun amend(driver: Driver) =
            on.engine.amend(driver)

        override fun add(ent: Ent) =
            on.engine.add(ent)

        override fun remove(ent: Ent) =
            on.engine.remove(ent)


        private fun dispatchFor(global: Long) {
            val old = lastSnapshotSlot
            val new = (global - shell.initializedTime) / frequency

            when {
                old == null -> {
                    sent.add(new)
                    receiver(new * frequency)
                    lastSnapshotSlot = new
                }
                new <= old -> {
                    sent.tailSet(new, true).forEach {
                        invalidate(it * frequency)
                    }
                }
                else -> {
                    sent.add(new)
                    receiver(new * frequency)
                    lastSnapshotSlot = new
                }
            }
        }

        override fun exchange(instruction: Instruction) {
            dispatchFor(instruction.time.global)
            on.engine.exchange(instruction)
        }

        override fun local(instructions: Sequence<Instruction>) {
            for (instruction in instructions) {
                dispatchFor(instruction.time.global)
                on.engine.local(sequenceOf(instruction))
            }
        }

        override fun capture(undo: () -> Unit) =
            on.engine.capture(undo)

        override fun invalidate(global: Long) {
            on.engine.invalidate(global)
            sent.headSet(global / frequency, false).clear()
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

    override fun load(shell: Shell, shellIn: ShellIn) =
        on.load(shell, shellIn)

    override fun store(shellOut: ShellOut) =
        on.store(shellOut)

    override var send: ((Instruction) -> Unit)?
        get() = on.send
        set(value) {
            on.send = value
        }

    override fun receive(instructions: Sequence<Instruction>) =
        on.receive(instructions)
}