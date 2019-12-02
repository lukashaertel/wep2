package eu.metatools.up.kryo.serializers

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output

object PairSerializer : Serializer<Pair<*, *>>(false, true) {
    override fun write(kryo: Kryo, output: Output, item: Pair<*, *>) {
        val firstClass = kryo.generics.nextGenericClass()

        if (firstClass != null && kryo.isFinal(firstClass))
            kryo.writeObject(output, item.first)
        else
            kryo.writeClassAndObject(output, item.first)

        kryo.generics.popGenericType()

        val secondClass = kryo.generics.nextGenericClass()

        if (secondClass != null && kryo.isFinal(secondClass))
            kryo.writeObject(output, item.second)
        else
            kryo.writeClassAndObject(output, item.second)

        kryo.generics.popGenericType()
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out Pair<*, *>>): Pair<*, *> {
        val firstClass = kryo.generics.nextGenericClass()

        val first = if (firstClass != null && kryo.isFinal(firstClass)) {
            val serializer = kryo.getSerializer(firstClass)
            kryo.readObjectOrNull(input, firstClass, serializer)
        } else
            kryo.readClassAndObject(input)

        kryo.generics.popGenericType()

        val secondClass = kryo.generics.nextGenericClass()

        val second = if (secondClass != null && kryo.isFinal(secondClass)) {
            val serializer = kryo.getSerializer(secondClass)
            kryo.readObjectOrNull(input, secondClass, serializer)
        } else
            kryo.readClassAndObject(input)

        kryo.generics.popGenericType()

        return Pair(first, second).also(kryo::reference)
    }
}