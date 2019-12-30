package eu.metatools.f2d.up.kryo.serializers

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import eu.metatools.f2d.data.Blend

object BlendSerializer : Serializer<Blend>(false, true) {
    override fun write(kryo: Kryo, output: Output, item: Blend) {
        output.writeInt(item.src)
        output.writeInt(item.dst)
        output.writeInt(item.srcAlpha)
        output.writeInt(item.dstAlpha)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out Blend>): Blend {
        val src = input.readInt()
        val dst = input.readInt()
        val srcAlpha = input.readInt()
        val dstAlpha = input.readInt()
        return Blend(src, dst, srcAlpha, dstAlpha)
    }
}