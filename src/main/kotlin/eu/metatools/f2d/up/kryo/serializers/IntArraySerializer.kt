package eu.metatools.f2d.up.kryo.serializers

import com.badlogic.gdx.utils.IntArray
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output

object IntArraySerializer : Serializer<IntArray>(false, false) {
    override fun write(kryo: Kryo, output: Output, item: IntArray) {
        output.writeInt(item.size, true)
        for (i in 0 until item.size)
            output.writeInt(item[i])
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out IntArray>): IntArray {
        val result = IntArray(input.readInt(true))

        repeat(result.size) {
            result[it] = input.readInt()
        }

        return result.also(kryo::reference)
    }
}