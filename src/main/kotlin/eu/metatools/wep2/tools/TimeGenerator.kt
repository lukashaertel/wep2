package eu.metatools.wep2.tools

import eu.metatools.wep2.coordinators.Coordinator
import eu.metatools.wep2.util.first
import eu.metatools.wep2.util.then
import java.io.Serializable

/**
 * Universal time exchangeable between systems.
 * @property playerCount The count of players determining the module.
 * @property time The wall-clock time.
 * @property player The player owning that time.
 * @property local The player-internal time.
 */
data class Time(
    val time: Long,
    val player: Short,
    val local: Byte
) : Comparable<Time>, Serializable {
    override fun compareTo(other: Time) =
        first(other) {
            time.compareTo(other.time)
        } then {
            player.compareTo(other.player)
        } then {
            local.compareTo(other.local)
        }

    override fun toString() =
        "$time@$player+$local"
}

/**
 * Generator for universal times.
 * @param playerCount The player count to use for time generation.
 * @param localIDs Scoped sequence for local IDs.
 */
class TimeGenerator(val localIDs: ScopedSequence<Long, Byte> = ScopedSequence(defaultLocalIDs)) {
    companion object {
        val defaultLocalIDs = generateSequence(0, Byte::inc)
    }

    /**
     * Consolidates the underlying generators.
     */
    fun consolidate(time: Long) {
        localIDs.consolidate(time)
    }

    /**
     * Takes a time usable to exchange.
     */
    fun take(time: Long, player: Short) =
        Time(time, player, localIDs.take(time))

}

/**
 * Uses the passed [timeGenerator] to send [player] time elements to the [coordinator],
 * using the given [player].
 */
fun <N> TickGenerator.tickToWith(
    timeGenerator: TimeGenerator,
    coordinator: Coordinator<N, Time>,
    name: N,
    time: Long,
    player: Short
) = tickTo(coordinator, name, time) {
    timeGenerator.take(it, player)
}