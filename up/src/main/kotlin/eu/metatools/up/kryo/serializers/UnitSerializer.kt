package eu.metatools.up.kryo.serializers

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output

object UnitSerializer : Serializer<Unit>(false, true) {
    override fun write(kryo: Kryo, output: Output, item: Unit) {
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out Unit>) {
    }
}