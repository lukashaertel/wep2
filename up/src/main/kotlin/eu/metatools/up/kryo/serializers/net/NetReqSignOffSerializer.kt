package eu.metatools.up.kryo.serializers.net

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import eu.metatools.up.net.NetReqSignOff

object NetReqSignOffSerializer : Serializer<NetReqSignOff>(false, true){
    override fun write(kryo: Kryo, output: Output, item: NetReqSignOff) {
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out NetReqSignOff>) =
        NetReqSignOff
}

