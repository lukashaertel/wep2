package eu.metatools.ex

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.serializers.DefaultSerializers
import eu.metatools.ex.ents.Movers
import eu.metatools.ex.ents.T2s
import eu.metatools.f2d.up.kryo.registerF2DSerializers
import eu.metatools.f2d.up.kryo.registerGDXSerializers
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

   // Register data objects.
   kryo.register(
       Movers::class.java,
       DefaultSerializers.EnumSerializer(
           Movers::class.java
       )
   )
   kryo.register(
       T2s::class.java,
       DefaultSerializers.EnumSerializer(
           T2s::class.java
       )
   )
}