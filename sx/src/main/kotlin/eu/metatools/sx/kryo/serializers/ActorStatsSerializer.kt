package eu.metatools.sx.kryo.serializers

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import eu.metatools.sx.ents.ActorStats

object ActorStatsSerializer : Serializer<ActorStats>(false, true) {
    override fun write(kryo: Kryo, output: Output, item: ActorStats) {
        output.writeDouble(item.hunger)
        output.writeDouble(item.fatigue)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out ActorStats>): ActorStats {
        val hunger = input.readDouble()
        val fatigue = input.readDouble()
        return ActorStats(hunger, fatigue)
    }
}