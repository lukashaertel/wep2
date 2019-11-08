package eu.metatools.wep2.nes.dt

import java.io.Serializable

data class Instruction(val name: String, val time: Time, val args: Any?) : Serializable {
    override fun toString() =
        "$time: $name($args)"
}