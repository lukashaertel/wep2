package eu.metatools.up.kryo.serializers.net

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import eu.metatools.up.dt.Inf
import eu.metatools.up.net.NetPing
import eu.metatools.up.net.NetReqClaims

object NetPingSerializer : Serializer<NetPing>(false, true) {
    override fun write(kryo: Kryo, output: Output, item: NetPing) {
        output.writeLong(item.time)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out NetPing>): NetPing {
        val time = input.readLong()
        return NetPing(time)
    }

}

