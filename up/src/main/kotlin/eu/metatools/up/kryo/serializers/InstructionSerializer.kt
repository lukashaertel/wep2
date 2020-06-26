package eu.metatools.up.kryo.serializers

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import eu.metatools.up.dt.Instruction
import eu.metatools.up.dt.Lx
import eu.metatools.up.dt.Time

object InstructionSerializer : Serializer<Instruction>(false, true) {
    override fun write(kryo: Kryo, output: Output, item: Instruction) {
        kryo.writeObject(output, item.target)
        output.writeByte(item.methodName)
        kryo.writeObject(output, item.time)
        output.writeInt(item.args.size, true)
        for (arg in item.args)
            kryo.writeClassAndObject(output, arg)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out Instruction>): Instruction {
        val target = kryo.readObject(input, Lx::class.java)
        val methodName = input.readByte()
        val time = kryo.readObject(input, Time::class.java)
        val argsLength = input.readInt(true)
        val args = List<Any?>(argsLength) {
            kryo.readClassAndObject(input)
        }

        return Instruction(target, methodName, time, args).also(kryo::reference)
    }
}