package eu.metatools.fio.up.kryo.serializers

import com.badlogic.gdx.graphics.Color
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output

object ColorSerializer : Serializer<Color>(false, false) {
    override fun write(kryo: Kryo, output: Output, item: Color) {
        output.writeInt(Color.rgba8888(item))
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out Color>): Color {
        val result = Color()
        Color.rgba8888ToColor(result, input.readInt())
        return result.also(kryo::reference)
    }
}