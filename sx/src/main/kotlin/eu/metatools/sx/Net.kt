package eu.metatools.sx

import com.esotericsoftware.kryo.Kryo
import eu.metatools.fig.kryo.registerF2DSerializers
import eu.metatools.fig.kryo.registerGDXSerializers
import eu.metatools.sx.ents.ActorStats
import eu.metatools.sx.ents.EatAt
import eu.metatools.sx.ents.GetTo
import eu.metatools.sx.kryo.serializers.ActorStatsSerializer
import eu.metatools.sx.kryo.serializers.EatAtSerializer
import eu.metatools.sx.kryo.serializers.GetToSerializer
import eu.metatools.up.kryo.registerKotlinSerializers
import eu.metatools.up.kryo.registerUpSerializers
import eu.metatools.up.kryo.setDefaults

fun configureKryo(kryo: Kryo) {
    // Add basic serialization.
    setDefaults(kryo)
    registerKotlinSerializers(kryo)
    registerUpSerializers(kryo)

    // Add graphics serialization.
    registerGDXSerializers(kryo)
    registerF2DSerializers(kryo)
    kryo.register(ActorStats::class.java, ActorStatsSerializer)
    kryo.register(GetTo::class.java, GetToSerializer)
    kryo.register(EatAt::class.java, EatAtSerializer)
}