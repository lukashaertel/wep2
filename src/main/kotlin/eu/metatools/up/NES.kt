package eu.metatools.up

import eu.metatools.up.aspects.*
import eu.metatools.up.basic.*
import eu.metatools.up.dsl.prop
import eu.metatools.up.dt.*
import eu.metatools.up.structure.Container
import eu.metatools.up.structure.Part

fun insSend(id: Lx, instruction: Instruction) {
    println("Sending $id.$instruction ")
}

lateinit var insReceive: (Lx, Instruction) -> Unit

fun main() {
    class S(on: Aspects?, id: Lx) : Ent(on, id) {
        var x by prop { 0 }

        var lastChild by prop<S?> { null }


        /**
         * If value is less than zero, resets, if greater, increments [x].
         */
        val inst = exchange(::doInst)
        private fun doInst() {
            if (x % 2 == 0)
                lastChild = constructed(S(on, id / "child" / time))
            x++
        }

        /**
         * Says hello to another entity.
         */
        val hello = exchange(::doHello)
        private fun doHello(s: S) {
            println("$id says hello to $s")
        }


        override fun toString(): String {
            return "S(id=$id, x=$x, lastChild=@${lastChild?.id})"
        }
    }

    val cet = mutableMapOf<Lx, Ent>()
    val data = mutableMapOf<Lx, Any?>()

    val scope = compose { on ->
        receive(object : Container {
            override val id: Lx
                get() = lx / "ROOT"

            override fun resolve(id: Lx): Part? {
                return cet[id]
            }

            override fun include(id: Lx, part: Part) {
                cet[id] = part as? Ent ?: error("Can only include entities")
                part.connect()
                println("Included $id=$part")
            }

            override fun exclude(id: Lx) {
                cet.remove(id)?.let {
                    println("Excluded $id=$it")
                    it.disconnect()
                }
            }
        })
        receive(RecursiveProxify(on, cet::getValue))
        receive(AssociativeStore(on, data))
        receive(FeedbackDispatch(on, ::insSend, ::insReceive::set))
        receive(CommitListener(on) { id, instruction, commit ->
            //println("id: $id, instruction: $instruction, commit: $commit")
        })
        receive(WarpPerformGuard(on))
    }
    val e = S(scope, lx / "A").also { cet[it.id] = it }
    e.connect()

    println(cet);println()
    e.inst(Time(5, 0, 0))
    println(cet);println()
    e.inst(Time(0, 0, 0))
    println(cet);println()
    e.inst(Time(15, 0, 0))
    println(cet);println()
    e.inst(Time(10, 0, 0))
    println(cet);println()

//    println(e)
//    e.inst(Time(10, 0, 0), 10)
//    println(e)
//    e.inst(Time(15, 0, 0), 3)
//    println(e)
//    e.inst(Time(5, 0, 0), -1)
//    println(e)
//    e.inst(Time(20, 0, 0), -2)
//    println(e)

    e.disconnect()
}
