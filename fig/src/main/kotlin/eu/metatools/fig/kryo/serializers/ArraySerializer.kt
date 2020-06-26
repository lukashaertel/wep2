package eu.metatools.fig.kryo.serializers

import com.badlogic.gdx.utils.Array
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output

object ArraySerializer : Serializer<Array<*>>(false, false) {
    override fun write(kryo: Kryo, output: Output, item: Array<*>) {
        output.writeInt(item.size, true)

        val valueClass = kryo.generics.nextGenericClass()

        if (valueClass != null && kryo.isFinal(valueClass))
            item.forEach { kryo.writeObject(output, it) }
        else
            item.forEach { kryo.writeClassAndObject(output, it) }

        kryo.generics.popGenericType()
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out Array<*>>): Array<*> {
        val result = Array<Any?>(input.readInt(true))

        val valueClass = kryo.generics.nextGenericClass()

        if (valueClass != null && kryo.isFinal(valueClass)) {
            val serializer = kryo.getSerializer(valueClass)
            repeat(result.size) {
                result[it] = kryo.readObjectOrNull(input, valueClass, serializer)
            }
        } else
            repeat(result.size) {
                result[it] = kryo.readClassAndObject(input)
            }

        kryo.generics.popGenericType()

        return result.also(kryo::reference)
    }
}