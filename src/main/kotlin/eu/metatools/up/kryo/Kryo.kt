package eu.metatools.up.kryo

import com.esotericsoftware.kryo.Kryo
import eu.metatools.up.dt.*
import eu.metatools.up.kryo.serializers.*
import java.util.*
import kotlin.reflect.KClass

/**
 * Creates the default Kotlin [Kryo] or extends [kryo].
 */
fun makeKryo(kryo: Kryo = Kryo()): Kryo {
    // Do not enforce registration.
    kryo.isRegistrationRequired = false

    // Basic Kotlin types.
    kryo.register(Unit::class.java, UnitSerializer)
    kryo.register(UUID::class.java, UUIDSerializer)
    kryo.register(Pair::class.java, PairSerializer)
    kryo.register(Triple::class.java, TripleSerializer)

    // KClass serializers.
    kryo.addDefaultSerializer(KClass::class.java, KClassSerializer)
    return kryo
}

/**
 * Creates the default Up [Kryo] or extends [kryo].
 */
fun makeUpKryo(kryo: Kryo = makeKryo()): Kryo {
    // Up types.
    kryo.register(Lx::class.java, LxSerializer)
    kryo.register(At::class.java, AtSerializer)
    kryo.register(Inf::class.java, InfSerializer)
    kryo.register(Inf::class.java, SupSerializer)
    kryo.register(Time::class.java, TimeSerializer)
    kryo.register(Instruction::class.java, InstructionSerializer)

    return kryo
}
