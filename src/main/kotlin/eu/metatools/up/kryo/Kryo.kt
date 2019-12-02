package eu.metatools.up.kryo

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.serializers.CollectionSerializer
import com.esotericsoftware.kryo.serializers.MapSerializer
import eu.metatools.up.dt.*
import eu.metatools.up.kryo.serializers.*
import java.util.*
import kotlin.reflect.KClass


fun makeKryo(): Kryo {
    val kryo = Kryo(SubtypeClassResolver(), null)

    // Basic Kotlin types.
    kryo.register(Unit::class.java, UnitSerializer)
    kryo.register(KClass::class.java, KClassSerializer)
    kryo.register(UUID::class.java, UUIDSerializer)
    kryo.register(Pair::class.java, PairSerializer)
    kryo.register(Triple::class.java, TripleSerializer)

    // Collection types.
    kryo.register(List::class.java, CollectionSerializer<List<*>>()).setInstantiator {
        arrayListOf<Any?>()
    }
    kryo.register(Set::class.java, CollectionSerializer<Set<*>>()).setInstantiator {
        hashSetOf<Any?>()
    }
    kryo.register(Map::class.java, MapSerializer<Map<*, *>>()).setInstantiator {
        hashMapOf<Any?, Any?>()
    }
    kryo.register(SortedSet::class.java, CollectionSerializer<SortedSet<*>>()).setInstantiator {
        TreeSet<Any?>()
    }
    kryo.register(SortedMap::class.java, MapSerializer<SortedMap<*, *>>()).setInstantiator {
        TreeMap<Any?, Any?>()
    }

    // Up types.
    kryo.register(Lx::class.java, LxSerializer)
    kryo.register(At::class.java, AtSerializer)
    kryo.register(Inf::class.java, InfSerializer)
    kryo.register(Inf::class.java, SupSerializer)
    kryo.register(Time::class.java, TimeSerializer)
    kryo.register(Instruction::class.java, InstructionSerializer)

    return kryo
}