package eu.metatools.up

import eu.metatools.up.aspects.*
import eu.metatools.up.basic.AssociativeStore
import eu.metatools.up.basic.FeedbackDispatch
import eu.metatools.up.basic.RecursiveProxify
import eu.metatools.up.dsl.map
import eu.metatools.up.dsl.prop
import eu.metatools.up.dsl.set
import eu.metatools.up.dt.*
import eu.metatools.up.lang.validate
import eu.metatools.up.notify.Event
import eu.metatools.up.notify.Handler

fun insSend(id: Lx, instruction: Instruction) {
    println("Sending $instruction on $id")
}

lateinit var insReceive: (Lx, Instruction) -> Unit

fun main() {
    class S(on: Aspects?, id: Lx, val bol: Int) : Ent(on, id), Args {
        override val extraArgs
            get() = fromValuesOf(::bol)

        var x by prop { 0 }

        var y by prop<S?> { null }

        val z by set { listOf(1, 2, 3) }

        val w by map { mapOf(1 to "no") }

        override fun perform(instruction: Instruction) {
            if (instruction.name == "inst") {
                println("Instruction with ${instruction.args}")
            }
        }

        override fun toString(): String {
            return "S(id=$id, bol=$bol, x=$x, y=@${y?.id}, z=$z, w=$w)"
        }


    }

    val cet = mutableMapOf<Lx, Ent>()
    val data = mutableMapOf<Lx, Any?>()

    val undoDeck = mutableMapOf<Lx, () -> Unit>()
    val changeDeck = mutableMapOf<Lx, Change<*>>()

    val scope = compose { on ->
        receive(RecursiveProxify(on, cet::getValue))
        receive(AssociativeStore(on, data))
        receive(FeedbackDispatch(on, ::insSend, ::insReceive::set))
        receive(object : Listen {
            override fun changed(id: Lx, change: Change<*>) =
                println("$id: $change")
        })
    }

//    object : With(), Dispatch, Listen, Track,
//        Store,
//        Proxify by RecursiveProxify(cet::getValue) {
//        val associativeStore = AssociativeStore(data)
//
//        override val handleReceive = Event<Lx, Instruction>()
//
//        override fun send(id: Lx, instruction: Instruction) {
//            println("$id sending $instruction")
//        }
//
//        override fun resetWith(id: Lx, undo: () -> Unit) {
//            // Put if not present.
//            undoDeck.getOrPut(id) { undo }
//        }
//
//        override fun viewed(id: Lx, value: Any?) {
//            println("Viewed $id, was $value")
//        }
//
//        override fun changed(id: Lx, change: Change<*>) {
//            changeDeck[id] = changeDeck[id]?.mergeForce(change) ?: change
//
//            println("Changed $id: $change")
//        }
//    }

    val e = S(scope, lx / "A", 3).also { cet[it.id] = it }

    e.connect()

    e.send(Instruction("inst", Time(0, 0, 0), "Henlo" to e))

    e.x = 1
    e.y = e
    e.x = 5
    e.z.addAll(listOf(7, 8, 9))
    e.z.retainAll(listOf(8, 6, 1))
    e.w[1] = "hello"
    e.w[2] = "xyz"
    e.w[2] = "abcd"

    scope<Store> {
        save()

        println("Save data")
        for ((k, v) in data)
            println("\t$k: $v")

        isLoading = true
        cet.values.forEach(Ent::disconnect)
        cet.clear()
        scope.reconstructPET(cet::set)
        cet.values.forEach(Ent::connect)
        println()
    }
}
