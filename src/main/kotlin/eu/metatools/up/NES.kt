package eu.metatools.up

import eu.metatools.up.aspects.*
import eu.metatools.up.basic.*
import eu.metatools.up.dsl.map
import eu.metatools.up.dsl.prop
import eu.metatools.up.dsl.set
import eu.metatools.up.dt.*

fun insSend(id: Lx, instruction: Instruction) {
    println("Sending $instruction on $id")
}

lateinit var insReceive: (Lx, Instruction) -> Unit

fun main() {
    class S(on: Aspects?, id: Lx, val bol: Int) : Ent(on, id), Args {
        override val extraArgs
            get() = fromValuesOf(::bol)

        var x by prop { 0 }

        var y by prop<S?> { null }

        val z by set { listOf(1, 2, 3) }

        val w by map { mapOf(1 to "no") }

        fun inst(arg: Int) {
            if (arg < 0)
                x = arg
            else
                x += arg
        }

        override fun perform(instruction: Instruction) =
            dispatch(instruction)

        override fun toString(): String {
            return "S(id=$id, bol=$bol, x=$x, y=@${y?.id}, z=$z, w=$w)"
        }
    }

    val cet = mutableMapOf<Lx, Ent>()
    val data = mutableMapOf<Lx, Any?>()

    val scope = compose { on ->
        receive(RecursiveProxify(on, cet::getValue))
        receive(AssociativeStore(on, data))
        receive(FeedbackDispatch(on, ::insSend, ::insReceive::set))
        receive(CommitListener(on) { id, instruction, commit ->
            println("id: $id, instruction: $instruction, commit: $commit")
        })
        receive(WarpPerformGuard(on))
    }
    val e = S(scope, lx / "A", 3).also { cet[it.id] = it }

    e.connect()
    println(e)
    e.send(Instruction("inst", Time(10, 0, 0), 3))
    println(e)
    e.send(Instruction("inst", Time(5, 0, 0), -1))
    println(e)
}
