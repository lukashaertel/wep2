package eu.metatools.up

import eu.metatools.up.aspects.*
import eu.metatools.up.basic.*
import eu.metatools.up.dsl.TimeSource
import eu.metatools.up.dsl.prop
import eu.metatools.up.dt.*
import eu.metatools.up.structure.Container
import eu.metatools.up.structure.Part
import eu.metatools.up.structure.connectIn

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

    val ts = TimeSource(scope, Short.MIN_VALUE)
    ts.connectIn {
        val e = S(scope, lx / "A").also { cet[it.id] = it }
        e.connect()
        // TODO: ^^^ This should be handles by some entity magic, ...

        ts.bind(1) {
            e.inst()
            e.inst()
            e.inst()
        }
        ts.bind(2) {
            e.inst()
            e.hello(e)
        }
        ts.bind(3) {
            e.inst()
        }
        ts.bind(3) {
            e.inst()
        }

        // TODO vvv ... as well as this.
        e.disconnect()

        scope.with<Store>()?.save()
        println(data)

        // TODO: While time is connected, save should for example store the time, or restore.
    }
}
