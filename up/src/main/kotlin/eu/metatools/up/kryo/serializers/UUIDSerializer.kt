package eu.metatools.up.kryo.serializers

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import java.util.*

object UUIDSerializer : Serializer<UUID>(false, true) {
    override fun write(kryo: Kryo, output: Output, item: UUID) {
        output.writeLong(item.mostSignificantBits)
        output.writeLong(item.leastSignificantBits)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out UUID>): UUID {
        val mostSignificantBits = input.readLong()
        val leastSignificantBits = input.readLong()
        return UUID(mostSignificantBits, leastSignificantBits)
    }
}