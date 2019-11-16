package eu.metatools.up.dt

import java.io.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.full.createType
import kotlin.reflect.full.functions
import kotlin.reflect.full.isSupertypeOf

data class Instruction(val name: String, val time: Time, val args: Any?) : Serializable {
    override fun toString() =
        "$time: $name($args)"
}

private val classToMemberMap = hashMapOf<Pair<KClass<*>, String>, (Time, Any?) -> Unit>()

fun Any.dispatch(instruction: Instruction) {
    // Get type.
    val receiverType = this::class

    // Get or create handler.
    val invoke = classToMemberMap.getOrPut(receiverType to instruction.name) {
        val functions = receiverType.functions
        functions.find {
            it.name == instruction.name && it.parameters.size == 3
        }?.let { { time: Time, arg: Any? -> it.call(this, time, arg);Unit } }
            ?: functions.find {
                it.name == instruction.name && it.parameters.size == 2
            }?.let { { _: Time, arg: Any? -> it.call(this, arg);Unit } }
            ?: functions.find {
                it.name == instruction.name && it.parameters.size == 1
            }?.let { { _: Time, _: Any? -> it.call(this);Unit } }
            ?: error("Unknown method $instruction.name")
    }

    // Invoke handler.
    invoke(instruction.time, instruction.args)
}