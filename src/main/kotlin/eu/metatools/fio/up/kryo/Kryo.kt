package eu.metatools.fio.up.kryo

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.FloatArray
import com.badlogic.gdx.utils.IntArray
import com.esotericsoftware.kryo.Kryo
import eu.metatools.fio.data.*
import eu.metatools.fio.up.kryo.serializers.*


/**
 * Creates the default lib GDX [Kryo] or extends [kryo].
 */
fun registerGDXSerializers(kryo: Kryo) {
    kryo.register(Array::class.java, ArraySerializer)
    kryo.register(IntArray::class.java, IntArraySerializer)
    kryo.register(FloatArray::class.java, FloatArraySerializer)
    kryo.register(Color::class.java, ColorSerializer)
}


/**
 * Creates the default F2D [Kryo] or extends [kryo].
 */
fun registerF2DSerializers(kryo: Kryo) {
    kryo.register(Blend::class.java, BlendSerializer)
    kryo.register(Cell::class.java, CellSerializer)
    kryo.register(Tri::class.java, TriSerializers)
    kryo.register(UTri::class.java, UTriSerializers)
    kryo.register(Hex::class.java, HexSerializers)
    kryo.register(UHex::class.java, UHexSerializers)
    kryo.register(Mat::class.java, MatSerializer)
    kryo.register(Pt::class.java, PtSerializer)
    kryo.register(Pts::class.java, PtsSerializer)
    kryo.register(Vec::class.java, VecSerializer)
    kryo.register(Vecs::class.java, VecsSerializer)
}