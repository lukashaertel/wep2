package eu.metatools.mk.tools

import eu.metatools.mk.coord.Coordinator

/**
 * Generates a sequence of uniform ticks in the given frequency.
 * @property initial The initial time, ticks will initially be generated between this and the given time.
 * @property frequency The tick frequency.
 */
class TickGenerator(val initial: Long, val frequency: Long) {
    /**
     * The last time the ticks were generated for.
     */
    private var lastTime = initial

    /**
     * Generates a sequence of ticks from the last generated time to this, respecting [initial] time and [frequency].
     */
    fun generateTicks(time: Long): LongProgression {
        // Check that time is linear.
        check(lastTime <= time) { "Time is not linear, last time was $lastTime, trying to generate ticks for $time" }

        // Compute components.
        val lastRelative = lastTime - initial
        val timeRelative = time - initial
        val firstOffset = lastRelative % frequency
        val firstTime = if (firstOffset == 0L) lastRelative else lastRelative + frequency - firstOffset

        // Update last time.
        lastTime = time

        // Return the progression.
        return firstTime until timeRelative step frequency
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
