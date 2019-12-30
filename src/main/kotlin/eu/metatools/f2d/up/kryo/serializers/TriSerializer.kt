package eu.metatools.f2d.up.kryo.serializers

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import eu.metatools.f2d.data.Tri

object TriSerializer : Serializer<Tri>(false, true) {
    override fun write(kryo: Kryo, output: Output, item: Tri) {
        output.writeInt(item.x)
        output.writeInt(item.y)
        output.writeInt(item.z)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out Tri>): Tri {
        val x = input.readInt()
        val y = input.readInt()
        val z = input.readInt()
        return Tri(x, y, z)
    }
}