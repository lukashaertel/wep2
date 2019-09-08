package eu.metatools.wep2.tools

import eu.metatools.wep2.coord.Coordinator

/**
 * Generates a sequence of uniform ticks in the given frequency.
 * @property initial The initial time, ticks will initially be generated between this and the given time.
 * @property frequency The tick frequency.
 */
class TickGenerator(val initial: Long, val frequency: Long) {
    companion object {
        /**
         * Restores a [TickGenerator] from the defining values of another tick generator.
         * @param initial Static or defining value of initial time.
         * @param frequency Static value of frequency.
         * @param lastTime Defining value of last generated tick set.
         */
        fun restore(initial: Long, frequency: Long, lastTime: Long): TickGenerator {
            // Create the resulting tick generator from the base values.
            val result = TickGenerator(initial, frequency)

            // Generate ticks into void, effectively resetting the base time.
            result.generateTicks(lastTime)

            // Return the tick generator in it's effective state.
            return result
        }
    }

    /**
     * The last time the ticks were generated for.
     */
    var lastTime = initial
        private set

    /**
     * Generates a sequence of ticks from the last generated time to this, respecting [initial] time and [frequency].
     */
    fun generateTicks(time: Long): LongProgression {
        // Check that time is linear.
        check(lastTime <= time) { "Time is not linear, last time was $lastTime, trying to generate ticks for $time" }

        // Todo: Verify this code, time discrepancies are not fun, might even need a dedicated data type to
        //  untangle this.

        // Compute components.
        val lastRelative = lastTime - initial
        val timeRelative = time - initial
        val firstOffset = lastRelative % frequency
        val firstTime = if (firstOffset == 0L) lastRelative else lastRelative + frequency - firstOffset

        // Update last time.
        lastTime = time

        // Return the progression.
        return initial + firstTime until initial + timeRelative step frequency
    }
}

/**
 * Takes the generated ticks of the [TickGenerator] and offers them to the [coordinator] as a triple of
 * [name], the tick time and [Unit] arguments.
 */
inline fun <N, T : Comparable<T>> TickGenerator.tickTo(
    coordinator: Coordinator<N, T>,
    name: N,
    time: Long,
    crossinline convert: (Long) -> T
) = coordinator.receiveAll(generateTicks(time).asSequence().map {
    Triple(name, convert(it), Unit)
})

/**
 * Standard version of [tickTo] with a coordinator taking integer numbers.
 */
@JvmName("tickToInt")
fun <N> TickGenerator.tickTo(
    coordinator: Coordinator<N, Int>,
    name: N,
    time: Long
) = tickTo(coordinator, name, time, Long::toInt)

/**
 * Standard version of [tickTo] with a coordinator taking long numbers.
 */
@JvmName("tickToLong")
fun <N> TickGenerator.tickTo(
    coordinator: Coordinator<N, Long>,
    name: N,
    time: Long
) = tickTo(coordinator, name, time, { it })
