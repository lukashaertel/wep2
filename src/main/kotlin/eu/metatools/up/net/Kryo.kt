package eu.metatools.up.net

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Registration
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.SerializerFactory
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.serializers.CollectionSerializer
import com.esotericsoftware.kryo.serializers.ImmutableSerializer
import com.esotericsoftware.kryo.serializers.MapSerializer
import com.esotericsoftware.kryo.util.DefaultClassResolver
import eu.metatools.up.dt.*
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.superclasses


fun makeKryo(): Kryo {
    val kryo = Kryo(object : DefaultClassResolver() {
        private val cached = hashMapOf<Class<*>, Registration?>()

        override fun getRegistration(type: Class<*>): Registration? = cached.getOrPut(type) {
            super.getRegistration(type) ?: type.kotlin.allSuperclasses
                .asSequence()
                .mapNotNull { getRegistration(it.java) }
                .firstOrNull()
        }
    }, null)

    kryo.register(Unit::class.java, object : Serializer<Unit>(false, true) {
        override fun write(kryo: Kryo, output: Output, item: Unit) {
        }

        override fun read(kryo: Kryo, input: Input, type: Class<out Unit>) {
        }
    })

    kryo.register(KClass::class.java, object : Serializer<KClass<*>>(false, true) {
        override fun write(kryo: Kryo, output: Output, item: KClass<*>) {
            output.writeString(item.java.name)
        }

        override fun read(kryo: Kryo, input: Input, type: Class<out KClass<*>>): KClass<*> {
            return kryo.classLoader.loadClass(input.readString()).kotlin.also(kryo::reference)
        }
    })

    kryo.register(Time::class.java, object : Serializer<Time>(false, true) {
        override fun write(kryo: Kryo, output: Output, item: Time) {
            output.writeLong(item.global)
            output.writeShort(item.player.toInt())
            output.writeByte(item.local)
        }

        override fun read(kryo: Kryo, input: Input, type: Class<out Time>): Time {
            val global = input.readLong()
            val player = input.readShort()
            val local = input.readByte()
            return Time(global, player, local).also(kryo::reference)
        }
    })
    kryo.register(Instruction::class.java, object : Serializer<Instruction>(false, true) {
        override fun write(kryo: Kryo, output: Output, item: Instruction) {
            output.writeByte(item.methodName)
            kryo.writeObject(output, item.time)
            output.writeInt(item.args.size, true)
            for (arg in item.args)
                kryo.writeClassAndObject(output, arg)
        }

        override fun read(kryo: Kryo, input: Input, type: Class<out Instruction>): Instruction {
            val methodName = input.readByte()
            val time = kryo.readObject(input, Time::class.java)
            val argsLength = input.readInt(true)
            val args = List<Any?>(argsLength) {
                kryo.readClassAndObject(input)
            }

            return Instruction(methodName, time, args).also(kryo::reference)
        }
    })
    kryo.register(Lx::class.java, object : Serializer<Lx>(false, true) {
        override fun write(kryo: Kryo, output: Output, item: Lx) {
            output.writeInt(item.nodes.size, true)
            for (node in item.nodes)
                kryo.writeClassAndObject(output, node)
        }

        override fun read(kryo: Kryo, input: Input, type: Class<out Lx>): Lx {
            val nodesLength = input.readInt(true)
            val nodes = List(nodesLength) {
                kryo.readClassAndObject(input) as Local
            }

            return Lx(nodes).also(kryo::reference)
        }
    })

    kryo.register(At::class.java, object : Serializer<At<*>>(false, true) {
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
    })

    kryo.register(Pair::class.java, object : Serializer<Pair<*, *>>(false, true) {
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
    })


    kryo.register(Inf::class.java, object : Serializer<Inf>(false, true) {
        override fun write(kryo: Kryo, output: Output, item: Inf) {
        }

        override fun read(kryo: Kryo, input: Input, type: Class<out Inf>) =
            Inf
    })

    kryo.register(Inf::class.java, object : Serializer<Sup>(false, true) {
        override fun write(kryo: Kryo, output: Output, item: Sup) {
        }

        override fun read(kryo: Kryo, input: Input, type: Class<out Sup>) =
            Sup
    })
    kryo.register(UUID::class.java, object : Serializer<UUID>(false, true) {
        override fun write(kryo: Kryo, output: Output, item: UUID) {
            output.writeLong(item.mostSignificantBits)
            output.writeLong(item.leastSignificantBits)
        }

        override fun read(kryo: Kryo, input: Input, type: Class<out UUID>): UUID {
            val mostSignificantBits = input.readLong()
            val leastSignificantBits = input.readLong()
            return UUID(mostSignificantBits, leastSignificantBits)
        }
    })

    kryo.register(Set::class.java, CollectionSerializer<Set<*>>()).setInstantiator {
        hashSetOf<Any?>()
    }
    kryo.register(List::class.java, CollectionSerializer<List<*>>()).setInstantiator {
        arrayListOf<Any?>()
    }
    kryo.register(Map::class.java, MapSerializer<Map<*, *>>()).setInstantiator {
        hashMapOf<Any?, Any?>()
    }

    // TODO: ^^^^ This is poo.
    return kryo
}