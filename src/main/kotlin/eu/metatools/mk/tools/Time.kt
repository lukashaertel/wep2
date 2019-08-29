package eu.metatools.mk.tools

import eu.metatools.mk.coord.Coordinator
import eu.metatools.mk.util.compareRound

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
) : Comparable<Time> {
    override fun compareTo(other: Time): Int {
        // Compare time and player as upper, return if not zero.
        val compareAcross = compareRound(time, player, other.time, other.player, playerCount)
        if (compareAcross != 0)
            return compareAcross

        // Same player at same time, return local.
        return local.compareTo(other.local)
    }

    override fun toString() =
        "$time,$local for $player/$playerCount"
}

/**
 * Generator for universal times.
 */
class TimeGenerator(val playerCount: Short) {
    /**
     * Generator for local identities.
     */
    private val scopedSequence = ScopedSequence<Long, Byte>(
        generateSequence(Byte.MIN_VALUE, Byte::inc)
    )

    /**
     * Consolidates the underlying generators.
     */
    fun consolidate(time: Long) {
        scopedSequence.consolidate(time)
    }

    /**
     * Takes a time usable to exchange.
     */
    fun take(time: Long, player: Short) =
        Time(playerCount, time, player, scopedSequence.take(time))

}

/**
 * Uses the passed [timeGenerator] to send _gaia_ time elements to the [coordinator],
 * using the last player number possible as player.
 */
fun <N> TickGenerator.tickToWith(
    timeGenerator: TimeGenerator,
    coordinator: Coordinator<N, Time>,
    name: N,
    time: Long
) = tickTo(coordinator, name, time) {
    timeGenerator.take(it, timeGenerator.playerCount.dec())
}