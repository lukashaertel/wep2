package eu.metatools.up

import eu.metatools.up.dsl.prop
import eu.metatools.up.dt.*
import eu.metatools.up.net.NetworkClock
import eu.metatools.up.net.makeNetwork
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

val executor = ScheduledThreadPoolExecutor(4)

fun main() {
    class S(scope: Scope, id: Lx) : Ent(scope, id) {
        var x by prop { 0 }

        var y by prop { 0 }

        var lastChild by prop<S?> { null }

        val update = repeating(1000, scope::initializedTime) {
            y++
            val y = y
            println("> Update y=$y")
            scope.capture(id / ".ctu") {
                println("< Update y=$y")
            }
        }

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
    val clock = NetworkClock(network, executor = executor)

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
        if (it.time.player == Short.MAX_VALUE)
            System.err.println("Leaked repeated instruction $it")
        network.instruction(it)
    }

    // Connect receiving to scope.
    onReceive = {
        scope.receive(it)
    }

    // If coordinating, create, otherwise restore.
    val root = if (network.isCoordinating) {
        // Create root, include.
        S(scope, lx / "root").also(scope::include)
    } else {
        // Restore, resolve root.
        val bundle = network.bundle()
        scope.loadFrom(bundle::get)
        scope.resolve(lx / "root") as S
    }

    val updater = executor.scheduleAtFixedRate({
        try {
            synchronized(scope) {
                root.update(clock.time)
                scope.invalidate(clock.time - 10_000L)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }, 0L, 1L, TimeUnit.SECONDS)

    println(scope.initializedTime)

    while (readLine() != "exit") {
        synchronized(scope) {
            scope.withTime(clock) {
                root.inst()
            }
        }
    }

    updater.cancel(false)

    // TODO: Probably not needed for now.
    root.disconnect()

    clock.close()
    network.close()
}
