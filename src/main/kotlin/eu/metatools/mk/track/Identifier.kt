package eu.metatools.mk.track

import eu.metatools.mk.tools.ReclaimableSequence
import eu.metatools.mk.util.labeledAs

/**
 * Wraps generating identities providing automated undo.
 */
interface Identifier<I> {
    /**
     * Claims an ID.
     */
    fun claim(): I

    /**
     * Releases an ID.
     */
    fun release(id: I)
}

/**
 * Marks an identity generator that partakes in undo-tracking.
 */
fun <I, R> identifier(reclaimableSequence: ReclaimableSequence<I, R>) = object : Identifier<Pair<I, R>> {
    override fun claim(): Pair<I, R> {
        val (id, undo) = reclaimableSequence.claim()
        undos.get()?.add(undo labeledAs {
            "releasing $id"
        })
        return id
    }

    override fun release(id: Pair<I, R>) {
        val undo = reclaimableSequence.release(id)
        undos.get()?.add(undo labeledAs {
            "reclaiming $id"
        })
    }
}

/**
 * Identifier generating short IDs with short recycle counts.
 */
fun identifierSmall(start: Short = 0) =
    identifier(ReclaimableSequence(generateSequence(start, Short::inc), 0, Short::inc))

/**
 * Identifier generating long IDs with int recycle counts.
 */
fun identifierBig(start: Long = 0L) =
    identifier(ReclaimableSequence(generateSequence(start, Long::inc), 0, Int::inc))

/**
 * Small identity.
 */
typealias SI = Pair<Short, Short>

/**
 * Big identity.
 */
typealias BI = Pair<Long, Int>