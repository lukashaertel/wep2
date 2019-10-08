package eu.metatools.wep2.tools

import eu.metatools.wep2.util.*
import java.util.*

/**
 * Tool for undoable synchronized ID claiming.
 * @param I The type of the primary identity.
 * @param R The type of the recycle counts.
 * @param sequence The sequence generating the new identities.
 * @param zero The zero value to use for the recycle counts.
 * @param inc How to increment the recycle counts.
 */
class ReclaimableSequence<I : Comparable<I>, R : Comparable<R>>(
    val sequence: Sequence<I>, val zero: R, val inc: (R) -> R
) {
    companion object {
        /**
         * Restores a [ReclaimableSequence] from the defining values of another scoped sequence.
         * @param sequence Static value of generating sequence.
         * @param zero Static value of non recycled [R] value.
         * @param inc Static value of incrementing a recycling cycle.
         * @param head Defining value of where source was at.
         * @param recycled Defining value of all recycled items.
         */
        fun <I : Comparable<I>, R : Comparable<R>> restore(
            sequence: Sequence<I>, zero: R, inc: (R) -> R, head: I?, recycled: List<ComparablePair<I, R>>
        ): ReclaimableSequence<I, R> {
            // Create the result sequence from the basic parameters.
            val result = ReclaimableSequence(sequence, zero, inc)

            // Advance while heads are not equal.
            while (result.generatorHead != head)
                result.claim()

            // Recycle everything that should be recycled.
            recycled.forEach {
                result.release(it)
            }

            // Return the result.
            return result
        }
    }

    /**
     * Instantiation of the generator iterator.
     */
    private val generator = sequence.iterator()

    /**
     * Last element that was generated or null.
     */
    var generatorHead: I? = null
        private set

    /**
     * Stack of currently recycling identities.
     */
    private val recycleStack = Stack<ComparablePair<I, R>>()

    /**
     * The currently recycled identities.
     */
    val recycled get() = recycleStack.toList()

    /**
     * claims an ID, returns the generator's next element on zero recycle counts or recycles an element.
     */
    fun claim(): Pair<ComparablePair<I, R>, () -> Unit> {
        // Recycle or get new ID, for new ID set recycle count to zero.
        val result = if (recycleStack.isEmpty())
            ComparablePair(generator.next().also { generatorHead = it }, zero)
        else
            recycleStack.pop().let { (id, rc) -> ComparablePair(id, inc(rc)) }

        // Return item paired with undo.
        return result to ({
            recycleStack.push(result)
            Unit
        } labeledAs {
            "recycling $result"
        })
    }

    /**
     * Releases the given [id], increments it's recycle count.
     */
    fun release(element: ComparablePair<I, R>): () -> Unit {
        // Add to bin, increment recycle counter.
        recycleStack.push(element)

        // Return popping the recycled element.
        return {
            recycleStack.pop()
            Unit
        } labeledAs {
            "pop recycle stack"
        }
    }
}