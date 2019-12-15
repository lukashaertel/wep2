package eu.metatools.f2d.up.kryo.serializers

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import eu.metatools.f2d.math.Real
import eu.metatools.f2d.math.RealPt

object RealPtSerializer : Serializer<RealPt>(false, true) {
    override fun write(kryo: Kryo, output: Output, item: RealPt) {
        output.writeInt(item.x.numerator)
        output.writeInt(item.y.numerator)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out RealPt>): RealPt {
        val xNumerator = input.readInt()
        val yNumerator = input.readInt()
        return RealPt(Real(xNumerator), Real(yNumerator))
    }
}