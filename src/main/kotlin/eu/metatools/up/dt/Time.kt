package eu.metatools.up.dt

import eu.metatools.wep2.util.first
import eu.metatools.wep2.util.then

/**
 * Three layer time object. Consists of the [global] time, the [player] number and the [local] time.
 */
data class Time(val global: Long, val player: Short, val local: Byte) : Comparable<Time> {

    companion object {
        /**
         * Minimal time value.
         */
        val MIN_VALUE = Time(Long.MIN_VALUE, Short.MIN_VALUE, Byte.MIN_VALUE)

        /**
         * Maximal time value.
         */
        val MAX_VALUE = Time(Long.MAX_VALUE, Short.MAX_VALUE, Byte.MAX_VALUE)
    }

    override fun compareTo(other: Time) =
        first(other) {
            global.compareTo(other.global)
        } then {
            player.compareTo(other.player)
        } then {
            local.compareTo(other.local)
        }

    override fun toString() =
        "$global-${player - Short.MIN_VALUE}-${local - Byte.MIN_VALUE}"
}