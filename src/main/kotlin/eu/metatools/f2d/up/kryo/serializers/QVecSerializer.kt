package eu.metatools.f2d.up.kryo.serializers

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import eu.metatools.f2d.data.Q
import eu.metatools.f2d.data.QPt
import eu.metatools.f2d.data.QVec

object QVecSerializer : Serializer<QVec>(false, true) {
    override fun write(kryo: Kryo, output: Output, item: QVec) {
        output.writeInt(item.x.numerator)
        output.writeInt(item.y.numerator)
        output.writeInt(item.z.numerator)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out QVec>): QVec {
        val xNumerator = input.readInt()
        val yNumerator = input.readInt()
        val zNumerator = input.readInt()
        return QVec(Q.fromNumerator(xNumerator), Q.fromNumerator(yNumerator), Q.fromNumerator(zNumerator))
    }
}