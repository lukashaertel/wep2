package eu.metatools.f2d.up.kryo.serializers

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import eu.metatools.f2d.math.Pt

object PtSerializer : Serializer<Pt>(false, true) {
    override fun write(kryo: Kryo, output: Output, item: Pt) {
        output.writeFloat(item.x)
        output.writeFloat(item.y)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out Pt>): Pt {
        val x = input.readFloat()
        val y = input.readFloat()
        return Pt(x, y)
    }
}