package eu.metatools.ex

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.serializers.DefaultSerializers
import eu.metatools.ex.ents.hero.Heroes
import eu.metatools.ex.ents.Blocks
import eu.metatools.ex.ents.Dyn
import eu.metatools.f2d.data.Vec
import eu.metatools.f2d.up.kryo.registerF2DSerializers
import eu.metatools.f2d.up.kryo.registerGDXSerializers
import eu.metatools.up.kryo.registerKotlinSerializers
import eu.metatools.up.kryo.registerUpSerializers
import eu.metatools.up.kryo.setDefaults


object DynSerializer : Serializer<Dyn>(false, true) {
    override fun write(kryo: Kryo, output: Output, item: Dyn) {
        kryo.writeObject(output, item.origin)
        kryo.writeObject(output, item.vel)
        output.writeDouble(item.time)
        output.writeDouble(item.limit)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out Dyn>): Dyn {
        val origin = kryo.readObject(input, Vec::class.java)
        val vel = kryo.readObject(input, Vec::class.java)
        val time = input.readDouble()
        val limit = input.readDouble()
        return Dyn(origin, vel, time, limit)
    }
}

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

    kryo.register(Dyn::class.java, DynSerializer)
}