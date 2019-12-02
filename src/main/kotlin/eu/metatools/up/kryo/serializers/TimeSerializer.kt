package eu.metatools.up.kryo.serializers

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import eu.metatools.up.dt.Time

object TimeSerializer : Serializer<Time>(false, true) {
    override fun write(kryo: Kryo, output: Output, item: Time) {
        output.writeLong(item.global)
        output.writeShort(item.player.toInt())
        output.writeByte(item.local)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out Time>): Time {
        val global = input.readLong()
        val player = input.readShort()
        val local = input.readByte()
        return Time(global, player, local).also(kryo::reference)
    }
}