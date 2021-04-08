package eu.metatools.up.kryo

import com.esotericsoftware.kryo.Kryo
import eu.metatools.up.dt.*
import eu.metatools.up.kryo.serializers.*
import eu.metatools.up.kryo.serializers.net.*
import eu.metatools.up.net.*
import java.util.*
import kotlin.reflect.KClass

fun setDefaults(kryo: Kryo) {
    // Do not enforce registration.
    kryo.isRegistrationRequired = false
    kryo.references = true
}

fun registerKotlinSerializers(kryo: Kryo) {
    // Basic Kotlin types.
    kryo.register(Unit::class.java, UnitSerializer)
    kryo.register(UUID::class.java, UUIDSerializer)
    kryo.register(Pair::class.java, PairSerializer)
    kryo.register(Triple::class.java, TripleSerializer)

    // KClass serializers.
    kryo.addDefaultSerializer(KClass::class.java, KClassSerializer)
}

fun registerUpSerializers(kryo: Kryo) {
    // Up types.
    kryo.register(Lx::class.java, LxSerializer)
    kryo.register(Time::class.java, TimeSerializer)
    kryo.register(Instruction::class.java, InstructionSerializer)

    // Up Network message types.
    kryo.register(NetReqClaims::class.java, NetReqClaimsSerializer)
    kryo.register(NetReqSignOff::class.java, NetReqSignOffSerializer)
    kryo.register(NetReqBundle::class.java, NetReqBundleSerializer)
    kryo.register(NetPing::class.java, NetPingSerializer)
    kryo.register(NetInstruction::class.java, NetInstructionSerializer)
    kryo.register(NetTouch::class.java, NetTouchSerializer)

    // Up Network data types.
    kryo.register(Claim::class.java, ClaimSerializer)
}
