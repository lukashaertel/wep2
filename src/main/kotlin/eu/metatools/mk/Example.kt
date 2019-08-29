package eu.metatools.mk

import eu.metatools.mk.coord.Warp
import eu.metatools.mk.tools.*
import eu.metatools.mk.track.*
import eu.metatools.mk.util.toComparable
import java.lang.IllegalArgumentException

class Child(context: Context<String, Time, SI>, val parent: SI? = null) :
    Entity<String, Time, SI>(context) {
    var money by prop(3)


    override fun evaluate(name: String, time: Time, args: Any?) =
        when (name) {
            "sib" -> rec {
                if (money > 0) {
                    // Decrement.
                    money--

                    // Create sibling.
                    Child(context, id)
                }
            }

            "earn" -> rec {
                money++
            }

            "kill" -> rec {
                delete()
            }

            else -> throw IllegalArgumentException("Unknown instruction $name")
        }

    override fun toString() =
        "(Child of $parent, money=$money)"
}


class Example : Warp<SN<String>, Time>() {
    val index = entityMap<String, Time, SI>()

    val ids = identifierSmall()

    /**
     * The context to use for entity creation.
     */
    val context = Context(this, index, ids)

    val root = Child(context)

    override fun evaluate(name: SN<String>, time: Time, args: Any?) =
        index.dispatchEvaluate(name, time, args)

    override fun toString() = "Index\r\n" + index.joinToString("\r\n") { "\t$it" }
}

fun main() {
    // The hosts with their respective variables.
    val hostA = Example()

    // Create clock generator and time converter.
    //val clock = TickGenerator(0, 50)
    val generator = TimeGenerator(10)

    hostA.root.signal("sib", generator.take(0, 0), Unit)
    hostA.root.signal("sib", generator.take(10, 0), Unit)
    hostA.root.signal("sib", generator.take(20, 0), Unit)
    hostA.root.signal("sib", generator.take(30, 0), Unit)
    hostA.root.signal("sib", generator.take(40, 0), Unit)
    hostA.root.signal("earn", generator.take(35, 0), Unit)

    hostA.root.signal("kill", generator.take(5, 0), Unit)
    hostA.index.minBy { it.key.first toComparable it.key.second }
        ?.value
        ?.signal("sib", generator.take(6, 0), Unit)
    println(hostA)
}