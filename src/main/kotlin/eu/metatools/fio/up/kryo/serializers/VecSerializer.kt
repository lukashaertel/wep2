package eu.metatools.fio.up.kryo.serializers

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import eu.metatools.fio.data.Vec

object VecSerializer : Serializer<Vec>(false, true) {
    override fun write(kryo: Kryo, output: Output, item: Vec) {
        output.writeFloat(item.x)
        output.writeFloat(item.y)
        output.writeFloat(item.z)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out Vec>): Vec {
        val x = input.readFloat()
        val y = input.readFloat()
        val z = input.readFloat()
        return Vec(x, y, z)
    }
}