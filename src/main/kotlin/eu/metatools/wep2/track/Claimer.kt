package eu.metatools.wep2.track

import eu.metatools.wep2.tools.ReclaimableSequence
import eu.metatools.wep2.tools.intRandom
import eu.metatools.wep2.util.labeledAs
import eu.metatools.wep2.util.uv
import java.util.*

/**
 * Wraps generating elements of a [ReclaimableSequence] providing automated undo.
 */
interface Claimer<T> {
    /**
     * Claims a value.
     */
    fun claim(): T

    /**
     * Releases a value.
     */
    fun release(value: T)
}

/**
 * Claims a value of a [Claimer], returns the first component without the recycle count.
 */
fun <I, R> Claimer<Pair<I, R>>.claimValue() =
    claim().first

// TODO: Not happy with the introduction of pair at this point.

/**
 * Marks a [Claimer] that wraps a [ReclaimableSequence] partakes in undo-tracking.
 */
fun <I, R> claimer(reclaimableSequence: ReclaimableSequence<I, R>) = object : Claimer<Pair<I, R>> {
    override fun claim(): Pair<I, R> {
        val (value, undo) = reclaimableSequence.claim()
        undos.get()?.add(undo labeledAs {
            "releasing $value"
        })
        return value
    }

    override fun release(value: Pair<I, R>) {
        val undo = reclaimableSequence.release(value)
        undos.get()?.add(undo labeledAs {
            "reclaiming $value"
        })
    }
}

/**
 * Small identity.
 */
typealias SI = Pair<Short, Short>

/**
 * Big identity.
 */
typealias BI = Pair<Long, Int>