package eu.metatools.up

import eu.metatools.up.dsl.prop
import eu.metatools.up.dt.*
import eu.metatools.up.net.NetworkClock
import eu.metatools.up.net.makeNetwork

fun main() {
    class S(scope: Scope, id: Lx) : Ent(scope, id) {
        var x by prop { 0 }

        var lastChild by prop<S?> { null }


        /**
         * If value is less than zero, resets, if greater, increments [x].
         */
        val inst = exchange(::doInst)

        private fun doInst() {
            if (x % 2 == 0)
                lastChild = constructed(S(scope, id / "child" / time)).also {
                    println("CONSTRUCTED $it")
                }
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

    // Network handlers.
    var onBundle: () -> Map<Lx, Any?> = { emptyMap() }
    var onReceive: (Instruction) -> Unit = {}

    // Create network and synchronized clock.
    val network = makeNetwork("NES", { onBundle() }, { onReceive(it) })
    val clock = NetworkClock(network)

    // Claim player.
    val player = network.claimSlot()

    // Initialize scope with player.
    val scope = StandardScope(player)

    // Connect bundling to scope.
    onBundle = {
        val result = hashMapOf<Lx, Any?>()
        scope.saveTo(result::set)
        result
    }

    // Connect sending to network.
    scope.onTransmit.register {
        network.instruction(it)
    }

    // Connect receiving to scope.
    onReceive = {
        scope.receive(it)
    }

    // If coordinating, create, otherwise restore.
    val root = if (network.isCoordinating) {
        // Create root, include.
        S(scope, lx / "root")
            .also(scope::include)
    } else {
        // Restore, resolve root.
        val bundle = network.bundle()
        scope.loadFrom(bundle::get)
        scope.resolve(lx / "root") as S
    }

    while (readLine() != "exit") {
        scope.withTime(clock) {
            root.inst()
        }
    }

    // TODO: Probably not needed for now.
    root.disconnect()

    clock.close()
    network.close()
}
