package eu.metatools.up.kryo.serializers

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import eu.metatools.up.dt.Sup

object SupSerializer : Serializer<Sup>(false, true) {
    override fun write(kryo: Kryo, output: Output, item: Sup) {
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out Sup>) =
        Sup
}