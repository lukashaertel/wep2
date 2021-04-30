package eu.metatools.sx.kryo.serializers

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import eu.metatools.sx.ents.GetTo
import kotlin.reflect.full.primaryConstructor

object GetToSerializer : Serializer<GetTo>(false, true) {
    override fun write(kryo: Kryo, output: Output, item: GetTo) {
        output.writeFloat(item.x)
        output.writeFloat(item.y)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out GetTo>): GetTo {
        val x = input.readFloat()
        val y = input.readFloat()
        return GetTo(x, y)
    }
}