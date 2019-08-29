package eu.metatools.mk.track

import eu.metatools.mk.util.labeledAs

/**
 * Thread local (implied parameter) tracking the current set of undo functions, which will in the end be reversed for
 * the computed undo method.
 */
internal val undos = ThreadLocal<MutableList<() -> Unit>>()

/**
 * Records all changes that are made to delgates that record to [undos].
 */
inline fun rec(block: () -> Unit): () -> Unit {
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

    // Undoing now is performing al undo operations in reversed order.
    return {
        target.asReversed().forEach {
            it()
        }
    } labeledAs {
        target.asReversed().joinToString(" ; ")
    }
}