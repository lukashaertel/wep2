package eu.metatools.up.kryo.serializers

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import eu.metatools.up.dt.Local
import eu.metatools.up.dt.Lx

object LxSerializer : Serializer<Lx>(false, true) {
    override fun write(kryo: Kryo, output: Output, item: Lx) {
        output.writeInt(item.nodes.size, true)
        for (node in item.nodes)
            kryo.writeClassAndObject(output, node)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out Lx>): Lx {
        val nodesLength = input.readInt(true)
        val nodes = List(nodesLength) {
            kryo.readClassAndObject(input) as Local
        }

        return Lx(nodes).also(kryo::reference)
    }
}