package eu.metatools.mk.tools

import java.util.*

/**
 * Tool for undoable synchronized ID claiming.
 * @param I The type of the primary identity.
 * @param R The type of the recycle counts.
 * @param sequence The sequence generating the new identities.
 * @param zero The zero value to use for the recycle counts.
 * @param inc How to increment the recycle counts.
 */
class ReclaimableSequence<I, R>(sequence: Sequence<I>, val zero: R, val inc: (R) -> R) {

    /**
     * Instantiation of the generator iterator.
     */
    private val generator = sequence.iterator()

    /**
     * Stack of currently recycling identities.
     */
    private val recycled = Stack<Pair<I, R>>()

    /**
     * claims an ID, returns the generator's next element on zero recycle counts or recycles an element.
     */
    fun claim(): Pair<Pair<I, R>, () -> Unit> {
        // Recycle or get new ID, for new ID set recycle count to zero.
        val result = if (recycled.isEmpty())
            generator.next() to zero
        else
            recycled.pop()

        // Return item paired with undo.
        return result to {
            recycled.push(result)
            Unit
        }
    }

    /**
     * Releases the given [id], increments it's recycle count.
     */
    fun release(element: Pair<I, R>): () -> Unit {
        // Add to bin, increment recycle counter.
        recycled.push(element.first to inc(element.second))

        // Return popping the recycled element.
        return {
            recycled.pop()
        }
    }
}