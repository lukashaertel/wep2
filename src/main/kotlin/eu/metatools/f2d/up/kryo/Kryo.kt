package eu.metatools.f2d.up.kryo

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.FloatArray
import com.badlogic.gdx.utils.IntArray
import com.esotericsoftware.kryo.Kryo
import eu.metatools.f2d.data.*
import eu.metatools.f2d.up.kryo.serializers.*


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
    kryo.register(Tri::class.java, TriSerializer)
    kryo.register(Mat::class.java, MatSerializer)
    kryo.register(Pt::class.java, PtSerializer)
    kryo.register(Pts::class.java, PtsSerializer)
    kryo.register(Q::class.java, QSerializer)
    kryo.register(QPt::class.java, QPtSerializer)
    kryo.register(Vec::class.java, VecSerializer)
    kryo.register(Vecs::class.java, VecsSerializer)
}