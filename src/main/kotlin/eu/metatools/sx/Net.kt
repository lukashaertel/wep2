package eu.metatools.sx

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import eu.metatools.fio.data.Vec
import eu.metatools.fio.up.kryo.registerF2DSerializers
import eu.metatools.fio.up.kryo.registerGDXSerializers
import eu.metatools.sx.ents.Flow
import eu.metatools.up.kryo.registerKotlinSerializers
import eu.metatools.up.kryo.registerUpSerializers
import eu.metatools.up.kryo.setDefaults


object FluidSerializer : Serializer<Flow>(false, true) {
    override fun write(kryo: Kryo, output: Output, item: Flow) {

        output.writeFloat(item.v.x)
        output.writeFloat(item.v.y)
        output.writeFloat(item.v.z)
        output.writeFloat(item.d)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out Flow>): Flow {
        val x = input.readFloat()
        val y = input.readFloat()
        val z = input.readFloat()
        val d = input.readFloat()

        return Flow(Vec(x, y, z), d)
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
    kryo.register(Flow::class.java, FluidSerializer)
}