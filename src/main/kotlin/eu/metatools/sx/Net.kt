package eu.metatools.sx

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import eu.metatools.fio.up.kryo.registerF2DSerializers
import eu.metatools.fio.up.kryo.registerGDXSerializers
import eu.metatools.sx.ents.Fluid
import eu.metatools.up.kryo.registerKotlinSerializers
import eu.metatools.up.kryo.registerUpSerializers
import eu.metatools.up.kryo.setDefaults


object FluidSerializer : Serializer<Fluid>(false, true) {
    override fun write(kryo: Kryo, output: Output, item: Fluid) {

        output.writeInt(item.mass)
        output.writeInt(item.left)
        output.writeInt(item.right)
        output.writeInt(item.front)
        output.writeInt(item.back)
        output.writeInt(item.above)
        output.writeInt(item.below)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out Fluid>): Fluid {
        val mass = input.readInt()
        val left = input.readInt()
        val right = input.readInt()
        val front = input.readInt()
        val back = input.readInt()
        val above = input.readInt()
        val below = input.readInt()

        return Fluid(
            mass,
            left,
            right,
            front,
            back,
            above,
            below
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
    kryo.register(Fluid::class.java, FluidSerializer)
}