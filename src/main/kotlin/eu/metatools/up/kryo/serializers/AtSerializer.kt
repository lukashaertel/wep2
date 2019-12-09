package eu.metatools.up.kryo.serializers

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import eu.metatools.up.dt.At

object AtSerializer : Serializer<At<*>>(false, true) {
    override fun write(kryo: Kryo, output: Output, item: At<*>) {
        kryo.writeClassAndObject(output, item.value)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out At<*>>): At<*> {
        @Suppress("unchecked_cast")
        val value = kryo.readClassAndObject(input) as Comparable<Any>
        return At(value).also(kryo::reference)
    }
}