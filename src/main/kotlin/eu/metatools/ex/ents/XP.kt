package eu.metatools.ex.ents

import kotlin.math.log10
import kotlin.math.pow

object XP {
    fun rangeFor(level: Int): IntRange {
        val a = 10.0.pow(level).toInt()
        val b = 10.0.pow(level.inc()).toInt()
        return a until b
    }

    fun levelFor(xp: Int) =
        if (xp <= 0) 0 else log10(xp.toDouble()).toInt()
}