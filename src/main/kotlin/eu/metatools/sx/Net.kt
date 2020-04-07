package eu.metatools.sx

import com.esotericsoftware.kryo.Kryo
import eu.metatools.fio.up.kryo.registerF2DSerializers
import eu.metatools.fio.up.kryo.registerGDXSerializers
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
}