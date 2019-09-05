package eu.metatools.wep2.examples

import eu.metatools.wep2.coord.Warp
import eu.metatools.wep2.tools.*
import eu.metatools.wep2.track.*

/**
 * Example of a coordinator that will be restored.
 * @param idgen The generator of IDs (in the example it will receive a properly initialized sequence).
 */
class RestorationExample(idgen: ReclaimableSequence<Short, Short>) : Warp<String, Time>() {
    /**
     * ID generator with undo tracking.
     */
    val ids = claimer(idgen)

    /**
     * Current value with undo tracking.
     */
    var value by prop(0)

    /**
     * Items created for the coordinator.
     */
    var items = map<SI, String>()

    override fun evaluate(name: String, time: Time, args: Any?) =
        when (name) {
            "inc" -> rec {
                // Increase value.
                value++
            }
            "set" -> rec {
                // Assign value.
                value = args as Int
            }
            "spawn" -> rec {
                // If valid value, crate new item.
                if (value > 0)
                    items[ids.claim()] = "Item at $value"
            }
            else -> { -> }
        }

    override fun toString() = "(value=$value, items=$items)"
}

/**
 * This method demonstrates restoration of nontrivial status.
 *
 * Although no explicit transport of data is given (values are assigned directly), the
 * behavior can be replicated by transferring the following parameters.
 *
 * * Head of the ID generator (a number)
 * * Recycled IDs (pairs of numbers)
 * * The absolute game status
 *    * The tracked *value* (a number)
 *    * The generated items (a map of IDs to strings)
 * * The instructions (triples of strings, times and arguments)
 * * The last time of the tick generator (a number)
 * * The scopes of the time generator (a map of long to byte)
 *
 * The genericity of the arguments may not be limiting, as they need to be transferable
 * via any desired transport anyways.
 */
fun main() {
    // Instantiate the first coordinator and advance it's state to something nontrivial.
    ///////////////////////////////////////////////////////////////////////////////////////////////

    // Create main set of entries.
    val idgen = shortNat()
    val coord = RestorationExample(idgen)
    val ticks = TickGenerator(0L, 1L)
    val time = TimeGenerator()

    // Do some ticking.
    ticks.tickToWith(time, coord, "inc", 5, 1, 0)

    // Consolidate some parts.
    coord.consolidate(time.take(2, 1, 0))
    time.consolidate(2)

    // Call another signal.
    coord.signal("spawn", time.take(4, 1, 0), Unit)

    // Here, the second coordinator will be restored by restoring all components.
    ///////////////////////////////////////////////////////////////////////////////////////////////


    // Reset the status as much as possible.
    coord.undoAll()


    // Restore the ID generator.
    val resIdgen = ReclaimableSequence.restore(
        idgen.sequence, idgen.zero, idgen.inc,
        idgen.generatorHead, idgen.recycled
    )

    // Begin restoring another coordinator, using the restored identity generator.
    val resCoord = RestorationExample(resIdgen)

    // Restore properties by "parcelled" values.
    resCoord.value = coord.value
    coord.items.forEach { (k, v) -> resCoord.items[k] = v }

    // Receive all instructions.
    resCoord.receiveAll(coord.instructions.asSequence())

    // Redo all instructions, now both coordinators have instructions that the may undo.
    coord.redoAll()


    // Restore tick generator from "parcelled" last time.
    val resTicks = TickGenerator.restore(ticks.initial, ticks.frequency, ticks.lastTime)

    // Restore time generator from "parcelled" scopes.
    val resTime = TimeGenerator(
        ScopedSequence.restore(time.localIDs.sequence, time.localIDs.scopes)
    )

    // Now, all components are restored, hereafter both coordinators are used.
    ///////////////////////////////////////////////////////////////////////////////////////////////

    coord.register(resCoord::receive)
    resCoord.register(coord::receive)

    // Show values.
    println(coord)
    println(resCoord)


    // Call another signal on restored coordinator.
    resCoord.signal("spawn", time.take(8, 1, 0), Unit)


    // Tick on both ends.
    ticks.tickToWith(time, coord, "inc", 10, 1, 0)
    resTicks.tickToWith(resTime, resCoord, "inc", 10, 1, 0)

    // Show values again.
    println(coord)
    println(resCoord)

    // Run a signal on the main coordinator, without undoing and redoing, this would
    // lead to inconsistent instruction caches.
    coord.signal("set", time.take(3, 1, 0), -100)

    // Show values.
    println(coord)
    println(resCoord)

    // Expected output:

    // (value=5, items={(0, 1)=Item at 5})
    // (value=5, items={(0, 1)=Item at 5})
    // (value=10, items={(0, 1)=Item at 5, (1, 1)=Item at 8})
    // (value=10, items={(0, 1)=Item at 5, (1, 1)=Item at 8})
    // (value=-94, items={})
    // (value=-94, items={})

}