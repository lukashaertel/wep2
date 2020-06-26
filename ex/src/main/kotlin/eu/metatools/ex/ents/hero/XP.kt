package eu.metatools.ex.ents.hero

import kotlin.math.log10
import kotlin.math.pow

/**
 * XP computations.
 */
object XP {
    /**
     * Determines the XP range for the given [level].
     */
    fun rangeFor(level: Int): IntRange {
        val a = 10.0.pow(level).toInt()
        val b = 10.0.pow(level.inc()).toInt()
        return a until b
    }

    /**
     * Determines the level for the given [xp].
     */
    fun levelFor(xp: Int) =
        if (xp <= 0) 0 else log10(xp.toDouble()).toInt()
}