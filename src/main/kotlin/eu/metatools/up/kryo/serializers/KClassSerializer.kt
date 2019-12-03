package eu.metatools.up.kryo.serializers

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import kotlin.reflect.KClass

object KClassSerializer : Serializer<KClass<*>>(false, true) {
    override fun write(kryo: Kryo, output: Output, item: KClass<*>) {
        kryo.writeObject(output, item.java)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out KClass<*>>): KClass<*> {
        return kryo.readObject(input, Class::class.java).kotlin.also(kryo::reference)
    }
}