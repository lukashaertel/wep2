package eu.metatools.up.kryo.serializers

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import eu.metatools.up.dt.*
import java.lang.IllegalArgumentException
import java.util.*

object LxSerializer : Serializer<Lx>(false, true) {
    private const val tagInf: Byte = 0
    private const val tagSup: Byte = 1
    private const val tagAtByte: Byte = 2
    private const val tagAtShort: Byte = 3
    private const val tagAtInt: Byte = 4
    private const val tagAtLong: Byte = 5
    private const val tagAtFloat: Byte = 6
    private const val tagAtDouble: Byte = 7
    private const val tagAtUUID: Byte = 8
    private const val tagAtString: Byte = 9
    private const val tagAtOther: Byte = 10
    override fun write(kryo: Kryo, output: Output, item: Lx) {
        output.writeInt(item.nodes.size, true)
        for (node in item.nodes)
            when (node) {
                is Inf -> output.writeByte(tagInf)
                is Sup -> output.writeByte(tagSup)
                is At<*> -> {
                    when (node.value) {
                        is Byte -> {
                            output.writeByte(tagAtByte)
                            output.writeByte(node.value)
                        }
                        is Short -> {
                            output.writeByte(tagAtShort)
                            output.writeShort(node.value.toInt())
                        }
                        is Int -> {
                            output.writeByte(tagAtInt)
                            output.writeInt(node.value)
                        }
                        is Long -> {
                            output.writeByte(tagAtLong)
                            output.writeLong(node.value)
                        }
                        is Float -> {
                            output.writeByte(tagAtFloat)
                            output.writeFloat(node.value)
                        }
                        is Double -> {
                            output.writeByte(tagAtDouble)
                            output.writeDouble(node.value)
                        }
                        is UUID -> {
                            output.writeByte(tagAtUUID)
                            kryo.writeObject(output, node.value)
                        }
                        is String -> {
                            output.writeByte(tagAtString)
                            output.writeString(node.value)
                        }
                        else -> {
                            output.writeByte(tagAtOther)
                            kryo.writeClassAndObject(output, node.value)
                        }
                    }
                }
            }
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out Lx>): Lx {
        val nodesLength = input.readInt(true)
        val nodes = List(nodesLength) {
            when (val tag = input.readByte()) {
                tagInf -> Inf
                tagSup -> Sup
                tagAtByte -> At(input.readByte())
                tagAtShort -> At(input.readShort())
                tagAtInt -> At(input.readInt())
                tagAtLong -> At(input.readLong())
                tagAtFloat -> At(input.readFloat())
                tagAtDouble -> At(input.readDouble())
                tagAtUUID -> At(kryo.readObject(input, UUID::class.java))
                tagAtString -> At(input.readString())
                tagAtOther -> At(kryo.readClassAndObject(input) as Comparable<Any?>)
                else -> throw IllegalArgumentException("Illegal tag $tag")
            }
        }

        return Lx(nodes).also(kryo::reference)
    }
}