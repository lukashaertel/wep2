package eu.metatools.wep2.track

import eu.metatools.wep2.tools.ReclaimableSequence
import eu.metatools.wep2.util.labeledAs

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
 * Reclaimable sequence generating short IDs with short recycle counts.
 */
fun smallSequence(start: Short = 0) =
    ReclaimableSequence(generateSequence(start, Short::inc), 0, Short::inc)

/**
 * Reclaimable sequence generating long IDs with int recycle counts.
 */
fun bigSequence(start: Long = 0L) =
    ReclaimableSequence(generateSequence(start, Long::inc), 0, Int::inc)

/**
 * Small identity.
 */
typealias SI = Pair<Short, Short>

/**
 * Big identity.
 */
typealias BI = Pair<Long, Int>