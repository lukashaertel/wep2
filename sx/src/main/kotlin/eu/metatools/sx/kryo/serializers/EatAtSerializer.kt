package eu.metatools.sx.kryo.serializers

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import eu.metatools.sx.ents.EatAt
import eu.metatools.up.dt.Lx
import eu.metatools.up.kryo.serializers.LxSerializer

object EatAtSerializer : Serializer<EatAt>(false, true) {
    override fun write(kryo: Kryo, output: Output, item: EatAt) {
        kryo.writeObject(output, item.id, LxSerializer)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out EatAt>): EatAt {
        val id = kryo.readObject(input, Lx::class.java, LxSerializer)
        return EatAt(id)
    }
}