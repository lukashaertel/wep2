package eu.metatools.wep2.examples

import eu.metatools.f2d.ex.undefined
import eu.metatools.wep2.aspects.saveToMap
import eu.metatools.wep2.components.claimer
import eu.metatools.wep2.components.prop
import eu.metatools.wep2.storage.Restore
import eu.metatools.wep2.system.StandardEntity
import eu.metatools.wep2.system.StandardSystem
import eu.metatools.wep2.tools.Time
import eu.metatools.wep2.components.randomInt
import eu.metatools.wep2.storage.restoreBy
import eu.metatools.wep2.track.rec
import eu.metatools.wep2.util.listeners.Listener
import eu.metatools.wep2.util.randomInts

/**
 * A child in a [StandardSystem].
 *
 * @property system The system that this child uses as a context an to generate randoms etc.
 * @param The restore object to use to restore values.
 */
class StandardChild(
    val system: StandardSystem<String>,
    restore: Restore?,
    value: () -> Int = undefined()
) : StandardEntity<String>(system, restore) {
    val rnd by claimer(randomInts(0L))

    /**
     * A restored or initialized value.
     */
    var value by prop(initial = value)

    /**
     * A random value.
     */
    var ar by prop { 0 }

    /**
     * Another random value.
     */
    var br by prop { 0 }

    override fun evaluate(name: String, time: Time, args: Any?) =
        when (name) {
            "inc" -> rec { value++ }
            "dec" -> rec { value-- }
            "sar" -> rec { ar = rnd.randomInt(0, 100) }
            "sbr" -> rec { br = rnd.randomInt(0, 100) }
            else -> { -> }
        }

    override fun toString() =
        "(StandardChild $id value=$value, ar=$ar, br=$br)"
}

fun main() {
    // The first standard system, freshly created..
    val a = StandardSystem<String>(
        null, { it },
        playerCountListener = Listener.CONSOLE
    )

    // A standard child in the system, do not restore here.
    val e = StandardChild(a, null) { 123 }

    // Signal some impulses.
    e.signal("inc", a.time(System.currentTimeMillis()), Unit)
    e.signal("inc", a.time(System.currentTimeMillis()), Unit)
    e.signal("inc", a.time(System.currentTimeMillis()), Unit)

    // Consolidate values.
    a.consolidate(System.currentTimeMillis())

    // Get complete data from the first system.
    val data = a.saveToMap()

    // The second standard system.
    val b = restoreBy(data::get) { restore ->
        StandardSystem<String>(restore, { it })
    }

    // Connect systems before running instructions.
    a.register(b::receive)
    b.register(a::receive)

    // Claim a new player ID for the new player before doing something.
    b.claimNewPlayer()

    // Find root entity.
    val f = b.index.values.filterIsInstance<StandardChild>().first()

    // Run signals on this entity.
    f.signal("dec", b.time(System.currentTimeMillis()), Unit)

    // Run randoms on both parties.
    e.signal("sar", a.time(System.currentTimeMillis()), Unit)
    f.signal("sbr", b.time(System.currentTimeMillis()), Unit)

    // Print the results.
    println(e)
    println(f)
}