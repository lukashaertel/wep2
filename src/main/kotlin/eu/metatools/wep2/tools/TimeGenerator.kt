package eu.metatools.wep2.tools

import eu.metatools.wep2.coord.Coordinator

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
        // Compare outer time, return if not zero.
        val compareTime = time.compareTo(other.time)
        if (compareTime != 0)
            return compareTime

        // Compare player, rotate for fair distribution.
        val mod = maxOf(playerCount, other.playerCount)
        val shift = (time + player) % mod
        val otherShift = (other.time + other.player) % mod
        val compareShift = shift.compareTo(otherShift)
        if (compareShift != 0)
            return compareShift

        // Same player on same time, return comparison of local.
        return local.compareTo(other.local)
    }

    override fun toString() =
        "$time.$local for $player/$playerCount"
}

/**
 * Generator for universal times.
 * @param playerCount The player count to use for time generation.
 * @param localIDs Scoped sequence for local IDs.
 */
class TimeGenerator(
    var playerCount: Short,
    val localIDs: ScopedSequence<Long, Byte> = ScopedSequence(generateSequence(Byte.MIN_VALUE, Byte::inc))
) {

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
        Time(playerCount, time, player, localIDs.take(time))

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