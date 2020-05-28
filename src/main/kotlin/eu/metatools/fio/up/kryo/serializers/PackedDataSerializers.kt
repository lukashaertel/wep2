package eu.metatools.fio.up.kryo.serializers

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import eu.metatools.fio.data.Hex
import eu.metatools.fio.data.Tri
import eu.metatools.fio.data.UHex
import eu.metatools.fio.data.UTri

object TriSerializers : Serializer<Tri>(false, true) {
    override fun write(kryo: Kryo, output: Output, item: Tri) {
        output.writeLong(item.packed)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out Tri>): Tri {
        val packed = input.readLong()
        return Tri(packed)
    }
}

object UTriSerializers : Serializer<UTri>(false, true) {
    override fun write(kryo: Kryo, output: Output, item: UTri) {
        output.writeLong(item.actual.packed)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out UTri>): UTri {
        val packed = input.readLong()
        return UTri(Tri(packed))
    }
}

object HexSerializers : Serializer<Hex>(false, true) {
    override fun write(kryo: Kryo, output: Output, item: Hex) {
        output.writeLong(item.packed)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out Hex>): Hex {
        val packed = input.readLong()
        return Hex(packed)
    }
}

object UHexSerializers : Serializer<UHex>(false, true) {
    override fun write(kryo: Kryo, output: Output, item: UHex) {
        output.writeLong(item.actual.packed)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out UHex>): UHex {
        val packed = input.readLong()
        return UHex(Hex(packed))
    }
}