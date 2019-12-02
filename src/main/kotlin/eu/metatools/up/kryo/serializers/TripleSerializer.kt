package eu.metatools.up.kryo.serializers

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output

object TripleSerializer : Serializer<Triple<*, *, *>>(false, true) {
    override fun write(kryo: Kryo, output: Output, item: Triple<*, *, *>) {
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

        val thirdClass = kryo.generics.nextGenericClass()

        if (thirdClass != null && kryo.isFinal(thirdClass))
            kryo.writeObject(output, item.third)
        else
            kryo.writeClassAndObject(output, item.third)

        kryo.generics.popGenericType()
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out Triple<*, *, *>>): Triple<*, *, *> {
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

        val thirdClass = kryo.generics.nextGenericClass()

        val third = if (thirdClass != null && kryo.isFinal(thirdClass)) {
            val serializer = kryo.getSerializer(thirdClass)
            kryo.readObjectOrNull(input, thirdClass, serializer)
        } else
            kryo.readClassAndObject(input)

        kryo.generics.popGenericType()

        return Triple(first, second, third).also(kryo::reference)
    }
}