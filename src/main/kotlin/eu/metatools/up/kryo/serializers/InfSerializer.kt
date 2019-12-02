package eu.metatools.up.kryo.serializers

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import eu.metatools.up.dt.Inf

object InfSerializer : Serializer<Inf>(false, true) {
    override fun write(kryo: Kryo, output: Output, item: Inf) {
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out Inf>) =
        Inf
}