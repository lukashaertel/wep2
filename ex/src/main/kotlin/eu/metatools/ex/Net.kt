package eu.metatools.ex

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.serializers.DefaultSerializers
import eu.metatools.ex.ents.Blocks
import eu.metatools.ex.ents.hero.Heroes
import eu.metatools.fig.kryo.registerF2DSerializers
import eu.metatools.fig.kryo.registerGDXSerializers
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
        Heroes::class.java,
        DefaultSerializers.EnumSerializer(
            Heroes::class.java
        )
    )
    kryo.register(
        Blocks::class.java,
        DefaultSerializers.EnumSerializer(
            Blocks::class.java
        )
    )
}