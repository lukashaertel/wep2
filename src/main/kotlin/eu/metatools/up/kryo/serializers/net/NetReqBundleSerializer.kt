package eu.metatools.up.kryo.serializers.net

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import eu.metatools.up.dt.Inf
import eu.metatools.up.net.NetReqBundle
import eu.metatools.up.net.NetReqClaims
import eu.metatools.up.net.NetReqOffset

object NetReqBundleSerializer : Serializer<NetReqBundle>(false, true){
    override fun write(kryo: Kryo, output: Output, item: NetReqBundle) {
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out NetReqBundle>) =
        NetReqBundle
}

