package eu.metatools.up.dt

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.*

/**
 * Three layer time object. Consists of the [global] time, the [player] number and the [local] time.
 */
data class Time(val global: Long, val player: Short, val local: Byte) : Comparable<Time> {
    companion object {
        /**
         * The formatter to use when printing the [instant].
         */
        private val formatter = DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR)
            .appendLiteral('-')
            .appendValue(ChronoField.DAY_OF_YEAR)
            .appendLiteral('-')
            .appendValue(ChronoField.HOUR_OF_DAY)
            .appendLiteral(':')
            .appendValue(ChronoField.MINUTE_OF_HOUR)
            .appendLiteral(':')
            .appendValue(ChronoField.SECOND_OF_MINUTE)
            .appendLiteral('.')
            .appendValue(ChronoField.MILLI_OF_SECOND)
            .toFormatter()
            .withLocale(Locale.ENGLISH)
            .withZone(ZoneId.of("UTC"))

        /**
         * Minimal time value.
         */
        val MIN_VALUE = Time(Long.MIN_VALUE, Short.MIN_VALUE, Byte.MIN_VALUE)
        /**
         * Maximal time value.
         */
        val MAX_VALUE = Time(Long.MAX_VALUE, Short.MAX_VALUE, Byte.MAX_VALUE)
    }

    override fun compareTo(other: Time): Int {
        val r1 = global.compareTo(other.global)
        if (r1 != 0) return r1
        val r2 = player.compareTo(other.player)
        if (r2 != 0) return r2
        return local.compareTo(other.local)
    }

    /**
     * The instant of the [global]. // TODO: Deal with time zone offsets (in general).
     */
    val instant: Instant
        get() = Instant.ofEpochMilli(global)

    override fun toString() =
        "${formatter.format(instant)}-${player - Short.MIN_VALUE}-${local - Byte.MIN_VALUE}"
}