package eu.metatools.up.kryo.serializers.net

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import eu.metatools.up.net.NetTouch
import java.util.*

object NetTouchSerializer : Serializer<NetTouch>(false, true) {
    override fun write(kryo: Kryo, output: Output, item: NetTouch) {
        kryo.writeObject(output, item.uuid)
        output.writeLong(item.time)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out NetTouch>): NetTouch {
        val uuid = kryo.readObject(input, UUID::class.java)
        val time = input.readLong()
        return NetTouch(uuid, time)
    }
}

