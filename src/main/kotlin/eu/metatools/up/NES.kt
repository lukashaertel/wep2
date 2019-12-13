package eu.metatools.up

import eu.metatools.up.dsl.prop
import eu.metatools.up.dt.*
import eu.metatools.up.net.NetworkClaimer
import eu.metatools.up.net.NetworkClock
import eu.metatools.up.net.makeNetwork
import java.util.*
import java.util.concurrent.TimeUnit

fun main() {
    class S(shell: Shell, id: Lx) : Ent(shell, id) {
        var x by prop { 0 }

        var y by prop { 0 }

        var lastChild by prop<S?> { null }

        val update = repeating(Short.MAX_VALUE, 1000, shell::initializedTime) {
            y++
            println(this)
        }

        /**
         * If value is less than zero, resets, if greater, increments [x].
         */
        val inst = exchange(::doInst)

        private fun doInst() {
            if (x % 2 == 0)
                lastChild = constructed(S(shell, id / "child" / time)).also {
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
    val claimer = NetworkClaimer(network, UUID.randomUUID())

    // Initialize scope with player.
    val shell = StandardShell(claimer.currentClaim)

    // Connect bundling to scope.
    onBundle = {
        val result = TreeMap<Lx, Any?>()
        shell.store(result::set)
        result
    }

    // Connect sending to network.
    shell.send = {
        if (it.time.player == Short.MAX_VALUE)
            System.err.println("Leaked repeated instruction $it")
        network.instruction(it)
    }

    // Connect receiving to scope.
    onReceive = {
        shell.receive(it)
    }

    // If coordinating, create, otherwise restore.
    val root = if (network.isCoordinating) {
        // Create root, include.
        S(shell, lx / "root").also {
            shell.engine.add(it)
        }
    } else {
        // Restore, resolve root.
        val bundle = network.bundle()
        shell.load(bundle::get)
        shell.resolve(lx / "root") as S
    }

    val updater = network.executor.scheduleAtFixedRate({
        if (shell.player != claimer.currentClaim)
            System.err.println("Warning: Claim for engine has changed, this should not happen.")

        try {
            synchronized(shell) {
                root.update(clock.time)
                shell.engine.invalidate(clock.time - 10_000L)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }, 0L, 1L, TimeUnit.SECONDS)

    println(shell.initializedTime)

    while (readLine() != "exit") {
        synchronized(shell) {
            shell.withTime(clock) {
                root.inst()
            }
        }
    }

    updater.cancel(false)

    clock.close()
    claimer.close()
    network.close()
}
