package eu.metatools.sx

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import eu.metatools.fio.up.kryo.registerF2DSerializers
import eu.metatools.fio.up.kryo.registerGDXSerializers
import eu.metatools.sx.ents.Flow
import eu.metatools.sx.lang.FP
import eu.metatools.up.kryo.registerKotlinSerializers
import eu.metatools.up.kryo.registerUpSerializers
import eu.metatools.up.kryo.setDefaults


object FluidSerializer : Serializer<Flow>(false, true) {
    override fun write(kryo: Kryo, output: Output, item: Flow) {

        output.writeShort(item.mass.value.toInt())
        output.writeShort(item.left.value.toInt())
        output.writeShort(item.right.value.toInt())
        output.writeShort(item.front.value.toInt())
        output.writeShort(item.back.value.toInt())
        output.writeShort(item.above.value.toInt())
        output.writeShort(item.below.value.toInt())
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out Flow>): Flow {
        val mass = input.readShort()
        val left = input.readShort()
        val right = input.readShort()
        val front = input.readShort()
        val back = input.readShort()
        val above = input.readShort()
        val below = input.readShort()

        return Flow(
            FP(mass),
            FP(left),
            FP(right),
            FP(front),
            FP(back),
            FP(above),
            FP(below)
        )
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