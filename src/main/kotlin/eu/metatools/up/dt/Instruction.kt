package eu.metatools.up.dt

import java.io.Serializable

data class Instruction(val name: String, val time: Time, val args: Any?) : Serializable {
    override fun toString() =
        "$time: $name($args)"
}