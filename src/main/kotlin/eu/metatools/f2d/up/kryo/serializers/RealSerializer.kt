package eu.metatools.f2d.up.kryo.serializers

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import eu.metatools.f2d.math.Real

object RealSerializer : Serializer<Real>(false, true) {
    override fun write(kryo: Kryo, output: Output, item: Real) {
        output.writeInt(item.numerator)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out Real>): Real {
        val numerator = input.readInt()
        return Real(numerator)
    }
}