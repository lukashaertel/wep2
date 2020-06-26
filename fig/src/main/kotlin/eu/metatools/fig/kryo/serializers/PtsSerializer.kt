package eu.metatools.fig.kryo.serializers

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import eu.metatools.fio.data.Pts

object PtsSerializer : Serializer<Pts>(false, true) {
    override fun write(kryo: Kryo, output: Output, item: Pts) {
        output.writeInt(item.values.size)
        item.values.forEach(output::writeFloat)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out Pts>): Pts {
        val size = input.readInt(true)
        return Pts(*FloatArray(size) {
            input.readFloat()
        })
    }
}