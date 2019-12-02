package eu.metatools.up.kryo.serializers

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import eu.metatools.up.dt.At

object AtSerializer : Serializer<At<*>>(false, true) {
    override fun write(kryo: Kryo, output: Output, item: At<*>) {
        val valueClass = kryo.generics.nextGenericClass()

        if (valueClass != null && kryo.isFinal(valueClass))
            kryo.writeObject(output, item.value)
        else
            kryo.writeClassAndObject(output, item.value)

        kryo.generics.popGenericType()
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out At<*>>): At<*> {
        val valueClass = kryo.generics.nextGenericClass()

        val value = if (valueClass != null && kryo.isFinal(valueClass)) {
            val serializer = kryo.getSerializer(valueClass)
            kryo.readObjectOrNull(input, valueClass, serializer)
        } else
            kryo.readClassAndObject(input)

        kryo.generics.popGenericType()

        @Suppress("unchecked_cast")
        return At(value as Comparable<Any>).also(kryo::reference)
    }
}