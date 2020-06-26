package eu.metatools.up.kryo.serializers

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output

object PairSerializer : Serializer<Pair<*, *>>(false, true) {
    override fun write(kryo: Kryo, output: Output, item: Pair<*, *>) {
        kryo.writeClassAndObject(output, item.first)
        kryo.writeClassAndObject(output, item.second)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out Pair<*, *>>): Pair<*, *> {
        val first = kryo.readClassAndObject(input)
        val second = kryo.readClassAndObject(input)
        return Pair(first, second).also(kryo::reference)
    }
}