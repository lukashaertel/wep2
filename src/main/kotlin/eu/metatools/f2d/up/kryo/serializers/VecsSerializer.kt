package eu.metatools.f2d.up.kryo.serializers

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import eu.metatools.f2d.data.Vecs

object VecsSerializer : Serializer<Vecs>(false, true) {
    override fun write(kryo: Kryo, output: Output, item: Vecs) {
        output.writeInt(item.values.size)
        item.values.forEach(output::writeFloat)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out Vecs>): Vecs {
        val size = input.readInt(true)
        return Vecs(*FloatArray(size) {
            input.readFloat()
        })
    }
}