package eu.metatools.mk

import eu.metatools.mk.coord.Warp
import eu.metatools.mk.tools.*
import eu.metatools.mk.track.prop
import eu.metatools.mk.track.rec

class RestorationExample(initial: Int) : Warp<Int, Time>() {
    var value by prop(initial)

    override fun evaluate(name: Int, time: Time, args: Any?) = rec {
        value += name
    }
}

fun main() {
    // Create main set of entries.
    val coord = RestorationExample(0)
    val ticks = TickGenerator(0L, 1L)
    val time = TimeGenerator(1)

    // Do some ticking.
    ticks.tickToWith(time, coord, 2, 5)

    // Consolidate some parts.
    coord.consolidate(time.take(2, 0))
    time.consolidate(2)


    // Reset the status as much as possible.
    coord.undoAll()

    // Begin restoring another warper, using the "parelled" value of the source.
    val resCoord = RestorationExample(coord.value)

    // Receive all instructions.
    resCoord.receiveAll(coord.instructions.asSequence())

    // Redo all instructions, now both warpers have instructions that the may undo.
    coord.redoAll()


    // Restore tick generator from "parcelled" last time.
    val resTicks = TickGenerator.restore(ticks.initial, ticks.frequency, ticks.lastTime)

    // Restore time generator from "parcelled" scopes.
    val resTime = TimeGenerator(
        time.playerCount,
        ScopedSequence.restore(time.localIDs.sequence, time.localIDs.scopes)
    )

    coord.register(resCoord::receive)
    resCoord.register(coord::receive)

    // Show values.
    println(coord.value)
    println(resCoord.value)

    // Tick on both ends.
    ticks.tickToWith(time, coord, 1, 10)
    resTicks.tickToWith(resTime, resCoord, 1, 10)

    // Show values again.
    println(coord.value)
    println(resCoord.value)

    // Run a signal on the main coordinator, without undoing and redoing, this would
    // lead to inconsistent instruction caches.
    coord.signal(-100, time.take(3, 0), Unit)

    // Show values.
    println(coord.value)
    println(resCoord.value)

}