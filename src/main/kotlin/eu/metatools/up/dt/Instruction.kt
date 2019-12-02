package eu.metatools.up.dt

/**
 * A method name.
 */
typealias MethodName = Byte

/**
 * Converts a number to a method name.
 */
fun Number.toMethodName() = toByte()

data class Instruction(val target: Lx, val methodName: MethodName, val time: Time, val args: List<Any?>) {
    override fun toString() =
        "$target.$methodName(${args.joinToString(", ")})@$time"
}
