package eu.metatools.fio.up.kryo.serializers

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import eu.metatools.fio.data.Mat

object MatSerializer : Serializer<Mat>(false, true) {
    override fun write(kryo: Kryo, output: Output, item: Mat) {
        output.writeInt(item.values.size)
        item.values.forEach(output::writeFloat)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out Mat>): Mat {
        val size = input.readInt(true)
        return Mat(FloatArray(size) {
            input.readFloat()
        })
    }
}