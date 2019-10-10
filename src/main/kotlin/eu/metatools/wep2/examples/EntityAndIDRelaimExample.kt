package eu.metatools.wep2.examples

import eu.metatools.wep2.components.claimer
import eu.metatools.wep2.components.prop
import eu.metatools.wep2.coordinators.Warp
import eu.metatools.wep2.entity.*
import eu.metatools.wep2.tools.Time
import eu.metatools.wep2.tools.TimeGenerator
import eu.metatools.wep2.track.rec
import eu.metatools.wep2.util.shorts

/**
 * An entity created by the coordinator or another entity.
 * @property parent The parent that generated this child, or null.
 */
class Child(context: Context<String, Time, SI>, val parent: SI? = null) :
    TrackingEntity<String, Time, SI>(context) {
    /**
     * The money of this child, needed to create another child.
     */
    var money by prop { 3 }


    override fun evaluate(name: String, time: Time, args: Any?) =
        when (name) {
            "sib" -> rec {
                // Check if money is enough.
                if (money > 0) {
                    // Decrement.
                    money--

                    // Create sibling.
                    Child(context, id)

                }
            }

            "earn" -> rec {
                // Increment money.
                money++
            }

            "kill" -> rec {
                // Delete this child.
                delete()
            }

            else -> throw IllegalArgumentException("Unknown instruction $name")
        }

    override fun toString() =
        "(Child of $parent, money=$money)"
}

/**
 * The main coordinator, dispatching instructions on it's index.
 */
class Example : Warp<SN<String>, Time>(), Context<String, Time, SI> {
    /**
     * The central entity index.
     */
    override val index by entityMap<String, Time, SI>()

    /**
     * The ID generator.
     */
    val ids by claimer(shorts())

    /**
     * The root, will be created equally for all coordinators.
     */
    val root = Child(this, null)

    override fun newId() =
        ids.claim()

    override fun releaseId(id: SI) =
        ids.release(id)

    override fun signal(identity: SI, name: String, time: Time, args: Any?) {
        signal(identity to name, time, args)
    }

    override fun evaluate(name: SN<String>, time: Time, args: Any?) =
        // Dispatch via index.
        index.dispatchEvaluate(name, time, args)

    override fun toString() = "Index\r\n" + index.joinToString("\r\n") { "\t$it" }
}

/**
 * This example shows how entities can be used and that identities are stable
 * even after recycling, i.e., instructions on semantically different entities
 * are not routed to entities with a recycled identity.
 */
fun main() {
    // The hosts with their respective variables.
    val hostA = Example()

    // Create time generator.
    val generator = TimeGenerator()

    // Create some children, if possible.
    hostA.root.signal("sib", generator.take(0, 0), Unit)
    hostA.root.signal("sib", generator.take(10, 0), Unit)
    hostA.root.signal("sib", generator.take(20, 0), Unit)
    hostA.root.signal("sib", generator.take(30, 0), Unit)
    hostA.root.signal("sib", generator.take(40, 0), Unit)

    println(hostA)

    // Add money before some instructions, re-allowing some creations.
    hostA.root.signal("earn", generator.take(35, 0), Unit)

    println(hostA)

    // Send a kill signal before most of the creations
    hostA.root.signal("kill", generator.take(5, 0), Unit)

    println(hostA)

    // Create a child on another entity, expected behavior is that
    // an ID is reclaimed, but differs in recycle count.
    hostA.index.minBy { it.key }
        ?.value
        ?.signal("sib", generator.take(6, 0), Unit)

    println(hostA)

    // Expected output:

    //Index
    //	(0, 0)=(Child of null, money=0)
    //	(1, 0)=(Child of (0, 0), money=3)
    //	(2, 0)=(Child of (0, 0), money=3)
    //	(3, 0)=(Child of (0, 0), money=3)
    //Index
    //	(0, 0)=(Child of null, money=0)
    //	(1, 0)=(Child of (0, 0), money=3)
    //	(2, 0)=(Child of (0, 0), money=3)
    //	(3, 0)=(Child of (0, 0), money=3)
    //	(4, 0)=(Child of (0, 0), money=3)
    //Index
    //	(1, 0)=(Child of (0, 0), money=3)
    //Index
    //	(0, 1)=(Child of (1, 0), money=3)
    //	(1, 0)=(Child of (0, 0), money=2)
}