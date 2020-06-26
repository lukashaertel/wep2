package eu.metatools.fig.kryo.serializers

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import eu.metatools.fio.data.Cell

object CellSerializer : Serializer<Cell>(false, true) {
    override fun write(kryo: Kryo, output: Output, item: Cell) {
        output.writeInt(item.x)
        output.writeInt(item.y)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out Cell>): Cell {
        val x = input.readInt()
        val y = input.readInt()
        return Cell(x, y)
    }
}