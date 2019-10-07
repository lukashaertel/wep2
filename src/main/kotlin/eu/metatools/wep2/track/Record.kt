package eu.metatools.wep2.track

import eu.metatools.wep2.util.labeledAs
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2

/**
 * Thread local (implied parameter) tracking the current set of undo functions, which will in the end be reversed for
 * the computed undo method.
 */
internal val undos = ThreadLocal<MutableList<() -> Unit>>()

/**
 * Records all changes that are made to delegates that record to [undos].
 */
inline fun rec(block: () -> Unit): () -> Unit {
    @Suppress("non_public_call_from_public_inline")
    check(undos.get() == null) { "Already running recording" }

    // Generate undo stack.
    val target = mutableListOf<() -> Unit>()

    // Set while running block.
    @Suppress("non_public_call_from_public_inline")
    undos.set(target)

    // Run the given block, collect all undos.
    block()

    // Remove after, it should not be set from anywhere else.
    @Suppress("non_public_call_from_public_inline")
    undos.remove()

    // Undoing now is performing all undo operations in reversed order.
    return {
        target.asReversed().forEach {
            it()
        }
    } labeledAs {
        target.asReversed().joinToString(" ; ")
    }
}

/**
 * Adds an explicit undo-operation, should be called when the corresponding do-operation is performed.
 */
fun undo(block: () -> Unit) {
    @Suppress("non_public_call_from_public_inline")
    undos.get()?.add(block)
}

/**
 * Utility to [rec] a call to a function taking no arguments.
 */
fun <T : Comparable<T>> rec(time: T, function: KFunction1<T, *>) = rec {
    function(time)
}

/**
 * Utility to [rec] a call to a function taking an argument, automatically casting that argument.
 */
fun <T : Comparable<T>, A> rec(time: T, arg: Any?, function: KFunction2<T, A, *>) = rec {
    function.call(time, arg)
}