package eu.metatools.up.net

import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output

/**
 * Message disambiguation key.
 */
enum class MessageType {
    /**
     * Lists all claims.
     */
    ClaimList,

    /**
     * Claim a slot.
     */
    ClaimSlot,

    /**
     * Release a slot.
     */
    ReleaseSlot,

    /**
     * Retrieve delta time.
     */
    DeltaTime,

    /**
     * Dispatch an instruction.
     */
    Instruction,

    /**
     * Bundle all data.
     */
    Bundle;

    companion object {
        private val values by lazy { values() }
        fun fromOrdinal(index: Int) = values[index]
    }
}

fun Output.writeType(messageType: MessageType) =
    writeByte(messageType.ordinal)

fun Input.readType() =
    MessageType.fromOrdinal(readByte().toInt())