package eu.metatools.sx.ents

import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.experimental.xor

/**
 * Direction set.
 * @property packed The packed representation.
 */
inline class Dirs(val packed: Byte) : Comparable<Dirs> {
    companion object {
        /**
         * Containing left (x negative).
         */
        val left = Dirs(0b1)

        /**
         * Containing right (x positive).
         */
        val right = Dirs(0b10)

        /**
         * Containing back (y negative).
         */
        val back = Dirs(0b100)

        /**
         * Containing front (y positive).
         */
        val front = Dirs(0b1000)

        /**
         * Containing under (z negative).
         */
        val under = Dirs(0b10000)

        /**
         * Containing over (z positive).
         */
        val over = Dirs(0b100000)

        /**
         * No direction.
         */
        val none = Dirs(0b0)

        /**
         * Containing [left] and [right].
         */
        val xs = left or right

        /**
         * Containing [back] and [front].
         */
        val ys = back or front

        /**
         * Containing [under] and [over].
         */
        val zs = under or over

        /**
         * Containing all directions.
         */
        val all = Dirs(0b111111)
    }

    /**
     * True if [left] is set in the receiver.
     */
    val hasLeft get() = and(left) != none

    /**
     * True if [right] is set in the receiver.
     */
    val hasRight get() = and(right) != none

    /**
     * True if [back] is set in the receiver.
     */
    val hasBack get() = and(back) != none

    /**
     * True if [front] is set in the receiver.
     */
    val hasFront get() = and(front) != none

    /**
     * True if [under] is set in the receiver.
     */
    val hasUnder get() = and(under) != none

    /**
     * True if [over] is set in the receiver.
     */
    val hasOver get() = and(over) != none

    /**
     * Gets the number of directions set.
     */
    val size
        get() = (packed and 1) + ((packed / 2) and 1) + ((packed / 4) and 1) +
                ((packed / 8) and 1) + ((packed / 16) and 1) + ((packed / 32) and 1)


    /**
     * Returns all but the receiver dirs.
     */
    fun inv() =
        Dirs(packed xor all.packed)

    /**
     * Returns the opposite dirs of the receiver.
     */
    operator fun unaryMinus(): Dirs {
        var result = none.packed
        if (hasLeft) result = result or right.packed
        if (hasRight) result = result or left.packed
        if (hasBack) result = result or front.packed
        if (hasFront) result = result or back.packed
        if (hasUnder) result = result or over.packed
        if (hasOver) result = result or under.packed
        return Dirs(result)
    }

    /**
     * Returns directions in both receiver and [other].
     */
    infix fun and(other: Dirs) =
        Dirs(packed and other.packed)

    /**
     * Returns directions in receiver or [other].
     */
    infix fun or(other: Dirs) =
        Dirs(packed or other.packed)

    /**
     * True if receiver contains all directions of [other].
     */
    operator fun contains(other: Dirs) =
        packed and other.packed == other.packed

    /**
     * Compares numerically.
     */
    override fun compareTo(other: Dirs) =
        packed.compareTo(other.packed)

    override fun toString() = buildString {
        append('{')
        if (hasLeft) append('L') else append(' ')
        if (hasRight) append('R') else append(' ')
        if (hasBack) append('B') else append(' ')
        if (hasFront) append('F') else append(' ')
        if (hasUnder) append("U") else append(' ')
        if (hasOver) append("O") else append(' ')
        append('}')
    }
}