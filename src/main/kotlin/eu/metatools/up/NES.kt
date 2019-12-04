package eu.metatools.up

import eu.metatools.up.dsl.prop
import eu.metatools.up.dt.*
import eu.metatools.up.net.NetworkClock
import eu.metatools.up.net.makeNetwork
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

val executor = ScheduledThreadPoolExecutor(4)

fun main() {
    class S(shell: Shell, id: Lx) : Ent(shell, id) {
        var x by prop { 0 }

        var y by prop { 0 }

        var lastChild by prop<S?> { null }

        val update = repeating(1000, shell::initializedTime) {
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
            // TODO: Super method can be used but generates messy indents.
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
    val engine = StandardEngine(player)

    // Connect bundling to scope.
    onBundle = {
        val result = hashMapOf<Lx, Any?>()
        engine.saveTo(result::set)
        result
    }

    // Connect sending to network.
    engine.onTransmit.register {
        if (it.time.player == Short.MAX_VALUE)
            System.err.println("Leaked repeated instruction $it")
        network.instruction(it)
    }

    // Connect receiving to scope.
    onReceive = {
        engine.receive(it)
    }

    // If coordinating, create, otherwise restore.
    val root = if (network.isCoordinating) {
        // Create root, include.
        S(engine, lx / "root").also(engine::add)
    } else {
        // Restore, resolve root.
        val bundle = network.bundle()
        engine.loadFrom(bundle::get)
        engine.resolve(lx / "root") as S
    }

    val updater = executor.scheduleAtFixedRate({
        try {
            synchronized(engine) {
                root.update(clock.time)
                engine.invalidate(clock.time - 10_000L)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }, 0L, 1L, TimeUnit.SECONDS)

    println(engine.initializedTime)

    while (readLine() != "exit") {
        synchronized(engine) {
            engine.withTime(clock) {
                root.inst()
            }
        }
    }

    updater.cancel(false)

    // TODO: Probably not needed for now.
    root.driver.disconnect()

    clock.close()
    network.close()
}
