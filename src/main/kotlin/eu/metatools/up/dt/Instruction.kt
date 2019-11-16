package eu.metatools.up.dt

import java.io.Serializable

/**
 * A method name.
 */
typealias MethodName = Byte

/**
 * Converts a number to a method name.
 */
fun Number.toMethodName() = toByte()

data class Instruction(val methodName: MethodName, val time: Time, val args: List<Any?>) : Serializable {
    override fun toString() =
        "$methodName(${args.joinToString(", ")})@$time"
}
