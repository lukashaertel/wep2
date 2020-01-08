package eu.metatools.f2d.up.kryo.serializers

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import eu.metatools.f2d.data.Q
import eu.metatools.f2d.data.QPt

object QPtSerializer : Serializer<QPt>(false, true) {
    override fun write(kryo: Kryo, output: Output, item: QPt) {
        output.writeInt(item.x.numerator)
        output.writeInt(item.y.numerator)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out QPt>): QPt {
        val xNumerator = input.readInt()
        val yNumerator = input.readInt()
        return QPt(Q.fromNumerator(xNumerator), Q.fromNumerator(yNumerator))
    }
}