package eu.metatools.up.kryo.serializers

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output

object TripleSerializer : Serializer<Triple<*, *, *>>(false, true) {
    override fun write(kryo: Kryo, output: Output, item: Triple<*, *, *>) {
        kryo.writeClassAndObject(output, item.first)
        kryo.writeClassAndObject(output, item.second)
        kryo.writeClassAndObject(output, item.third)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out Triple<*, *, *>>): Triple<*, *, *> {
        val first = kryo.readClassAndObject(input)
        val second = kryo.readClassAndObject(input)
        val third = kryo.readClassAndObject(input)
        return Triple(first, second, third).also(kryo::reference)
    }
}