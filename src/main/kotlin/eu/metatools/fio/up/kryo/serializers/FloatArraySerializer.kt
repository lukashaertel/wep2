package eu.metatools.fio.up.kryo.serializers

import com.badlogic.gdx.utils.FloatArray
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output

object FloatArraySerializer : Serializer<FloatArray>(false, false) {
    override fun write(kryo: Kryo, output: Output, item: FloatArray) {
        output.writeInt(item.size, true)
        for (i in 0 until item.size)
            output.writeFloat(item[i])
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out FloatArray>): FloatArray {
        val result = FloatArray(input.readInt(true))

        repeat(result.size) {
            result[it] = input.readFloat()
        }

        return result.also(kryo::reference)
    }
}