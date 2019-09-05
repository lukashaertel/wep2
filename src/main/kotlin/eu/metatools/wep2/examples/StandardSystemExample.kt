package eu.metatools.wep2.examples

import eu.metatools.wep2.entity.bind.Restore
import eu.metatools.wep2.system.StandardEntity
import eu.metatools.wep2.system.StandardSystem
import eu.metatools.wep2.tools.Time
import eu.metatools.wep2.track.bind.prop
import eu.metatools.wep2.track.claimValue
import eu.metatools.wep2.track.rec
import eu.metatools.wep2.util.within

/**
 * A child in a [StandardSystem].
 *
 * @property system The system that this child uses as a context an to generate randoms etc.
 * @param The restore object to use to restore values.
 */
class StandardChild(
    val system: StandardSystem<String, Int>,
    restore: Restore?
) : StandardEntity<String>(system, restore) {
    /**
     * A restored or initialized value.
     */
    var value by prop(restore) { system.parameter }

    /**
     * A random value.
     */
    var ar by prop(restore) { 0 }

    /**
     * Another random value.
     */
    var br by prop(restore) { 0 }

    override fun evaluate(name: String, time: Time, args: Any?) =
        when (name) {
            "inc" -> rec { value++ }
            "dec" -> rec { value-- }
            "sar" -> rec { ar = system.randoms.claimValue().within(0, 100) }
            "sbr" -> rec { br = system.randoms.claimValue().within(0, 100) }
            else -> { -> }
        }

    override fun toString() =
        "(StandardChild $id value=$value, ar=$ar, br=$br)"
}

fun main() {
    // The first standard system with some parameters.
    val a = StandardSystem<String, Int>(0L, 123, null)

    // A standard child in the system, do not restore here.
    val e = StandardChild(a, null)

    // Signal some impulses.
    e.signal("inc", a.time(System.currentTimeMillis()), Unit)
    e.signal("inc", a.time(System.currentTimeMillis()), Unit)
    e.signal("inc", a.time(System.currentTimeMillis()), Unit)

    // Consolidate values.
    a.consolidate(System.currentTimeMillis())

    // The second standard system, has it's own parameters but they will be restored.
    val b = StandardSystem(0L, 432, a.save())

    // Connect systems before running instructions.
    a.register(b::receive)
    b.register(a::receive)

    // Claim a new player ID for the new player before doing something.
    b.claimNewPlayer(System.currentTimeMillis())

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