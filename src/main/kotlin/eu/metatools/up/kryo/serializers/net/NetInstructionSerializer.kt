package eu.metatools.up.kryo.serializers.net

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import eu.metatools.up.dt.Inf
import eu.metatools.up.dt.Instruction
import eu.metatools.up.net.NetInstruction
import eu.metatools.up.net.NetReqClaims

object NetInstructionSerializer : Serializer<NetInstruction>(false, true) {
    override fun write(kryo: Kryo, output: Output, item: NetInstruction) {
        kryo.writeObject(output, item.instruction)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out NetInstruction>) =
        NetInstruction(kryo.readObject(input, Instruction::class.java))
}

