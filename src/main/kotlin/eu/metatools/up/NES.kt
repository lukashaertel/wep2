package eu.metatools.up

import eu.metatools.up.aspects.*
import eu.metatools.up.basic.*
import eu.metatools.up.dsl.TimeSource
import eu.metatools.up.dsl.prop
import eu.metatools.up.dt.*
import eu.metatools.up.net.Network
import eu.metatools.up.net.makeNetwork
import eu.metatools.up.structure.Container
import eu.metatools.up.structure.Part
import eu.metatools.up.structure.connectIn
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

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

    lateinit var nw: Network


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
        nw = makeNetwork("nes", {
            on.with<Store>()?.save()
            data
        })
        receive(FeedbackDispatch(on, nw::instruction) {
            nw.received.register(it) // TODO: No disconnect.
        })
        receive(WarpPerformGuard(on))
    }

    val player = nw.claimSlot()

    val ts = TimeSource(scope, player)
    var deltaTime = 0L

    val updater = ScheduledThreadPoolExecutor(1).scheduleAtFixedRate({
        deltaTime = nw.deltaTime()
    }, 0, 3, TimeUnit.SECONDS)

    ts.connectIn {
        // TODO: Figure out nice way to activate store loading.
        nw.bundle().let {
            if (it !== data)
                scope<Store> {
                    isLoading = true
                    scope.reconstructPET(cet::set)
                    isLoading = false
                }
        }

        val eid = lx / "A"
        val e = cet[eid] as? S
            ?: S(scope, eid).also { cet[it.id] = it }

        e.connect()
        // TODO: ^^^ This should be handles by some entity magic, ...

        while (readLine() != "exit") {
            ts.bind(System.currentTimeMillis() + deltaTime) {
                e.inst()
            }
        }

        // TODO vvv ... as well as this.
        e.disconnect()
        // TODO: While time is connected, save should for example store the time, or restore.
    }

    updater.cancel(false)
    nw.close()
}
