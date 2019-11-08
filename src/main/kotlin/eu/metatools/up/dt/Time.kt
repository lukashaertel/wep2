package eu.metatools.wep2.nes.dt

import eu.metatools.wep2.util.first
import eu.metatools.wep2.util.then
import java.io.Serializable

/**
 * Three layer time object. Consists of the [global] time, the [player] number and the [local] time.
 */
data class Time(val global: Long, val player: Short, val local: Byte) : Comparable<Time>, Serializable {
    override fun compareTo(other: Time) =
        first(other) {
            global.compareTo(other.global)
        } then {
            player.compareTo(other.global)
        } then {
            local.compareTo(other.local)
        }

    override fun toString() =
        "$global/$player/$local"
}