package eu.metatools.f2d.up.kryo.serializers

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import eu.metatools.f2d.math.Mat

object MatSerializer : Serializer<Mat>(false, true) {
    override fun write(kryo: Kryo, output: Output, item: Mat) {
        item.values.forEach(output::writeFloat)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out Mat>): Mat {
        val m00 = input.readFloat()
        val m01 = input.readFloat()
        val m02 = input.readFloat()
        val m03 = input.readFloat()
        val m10 = input.readFloat()
        val m11 = input.readFloat()
        val m12 = input.readFloat()
        val m13 = input.readFloat()
        val m20 = input.readFloat()
        val m21 = input.readFloat()
        val m22 = input.readFloat()
        val m23 = input.readFloat()
        val m30 = input.readFloat()
        val m31 = input.readFloat()
        val m32 = input.readFloat()
        val m33 = input.readFloat()
        return Mat(m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23, m30, m31, m32, m33)
    }
}