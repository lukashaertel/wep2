package eu.metatools.up.kryo.serializers.net

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import eu.metatools.up.net.Claim
import java.util.*

object ClaimSerializer : Serializer<Claim>(false, true) {
    override fun write(kryo: Kryo, output: Output, item: Claim) {
        output.writeShort(item.id.toInt())
        output.writeLong(item.expires)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out Claim>): Claim {
        val id = input.readShort()
        val expires = input.readLong()
        return Claim(id, expires)
    }
}

