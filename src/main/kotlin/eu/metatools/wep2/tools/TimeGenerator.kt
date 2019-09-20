package eu.metatools.wep2.tools

import eu.metatools.wep2.coord.Coordinator
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
    val playerCount: Short,
    val time: Long,
    val player: Short,
    val local: Byte
) : Comparable<Time>, Serializable {
    override fun compareTo(other: Time) =
        first(other) {
            // Compare outer time.
            time.compareTo(other.time)
        } then {
            val mod = maxOf(playerCount, other.playerCount)
            val shift = (time + player) % mod
            val otherShift = (other.time + other.player) % mod
            // Compare player, rotate for fair distribution.
            shift.compareTo(otherShift)
        } then {
            // Same player on same time, return comparison of local.
            local.compareTo(other.local)
        }

    override fun toString() =
        "$time.$local for $player/$playerCount"
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
    fun take(time: Long, playerCount: Short, player: Short) =
        Time(playerCount, time, player, localIDs.take(time))

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
    playerCount: Short,
    player: Short
) = tickTo(coordinator, name, time) {
    timeGenerator.take(it, playerCount, player)
}