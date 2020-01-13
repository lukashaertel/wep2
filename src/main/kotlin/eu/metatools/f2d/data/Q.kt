package eu.metatools.f2d.data

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Coerces the value of the receiver in the range of valid [Int] values.
 */
private fun Long.coerceToInt() =
    coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()

/**
 * A fixed precision decimal number.
 */
class Q private constructor(unit: Unit, val numerator: Int) : Number(), Comparable<Q> {
    constructor(number: Number) : this(
        Unit, when (number) {
            is Q -> number.numerator
            is Byte -> number * precision
            is Short -> number * precision
            is Int -> number * precision
            is Long -> (number * precision).coerceToInt()
            is Float -> (number * precision).roundToInt()
            is Double -> (number * precision).roundToInt()
            else -> (number.toDouble() * precision).roundToInt()
        }
    )

    companion object {
        /**
         * Returns a [Q] with the specified [numerator].
         */
        fun fromNumerator(numerator: Int) =
            Q(Unit, numerator)

        /**
         * The denominator value.
         */
        const val precision = 3600

        /**
         * The square root of the denominator value.
         */
        const val sqrtPrecision = 60

        /**
         * Zero.
         */
        val ZERO = fromNumerator(0)

        /**
         * One.
         */
        val ONE = fromNumerator(precision)

        /**
         * Two.
         */
        val TWO = fromNumerator(2 * precision)

        /**
         * A third.
         */
        val THIRD = fromNumerator(precision / 3)

        /**
         * A half.
         */
        val HALF = fromNumerator(precision / 2)

        /**
         * Smallest non-zero positive number.
         */
        val E = fromNumerator(1)

        /**
         * The minimum value.
         */
        val MIN_VALUE = Q(Int.MIN_VALUE)

        /**
         * The maximum value.
         */
        val MAX_VALUE = Q(Int.MAX_VALUE)

        fun hypot(x: Q, y: Q) =
            (x * x + y + y).sqrt()
    }

    operator fun dec() =
        fromNumerator(numerator - precision)

    operator fun inc() =
        fromNumerator(numerator + precision)

    override operator fun compareTo(other: Q) =
        numerator.compareTo(other.numerator)

    operator fun div(other: Q): Q {
        val outNumerator = precision.toLong() * numerator.toLong() / other.numerator.toLong()
        return fromNumerator(outNumerator.coerceToInt())
    }

    operator fun minus(other: Q): Q {
        val outNumerator = numerator.toLong() - other.numerator.toLong()
        return fromNumerator(outNumerator.coerceToInt())
    }

    operator fun plus(other: Q): Q {
        val outNumerator = numerator.toLong() + other.numerator.toLong()
        return fromNumerator(outNumerator.coerceToInt())
    }

    operator fun times(other: Q): Q {
        val outNumerator = (numerator.toLong() * other.numerator.toLong()) / precision.toLong()
        return fromNumerator(outNumerator.coerceToInt())
    }

    operator fun unaryMinus() = fromNumerator(-numerator)

    operator fun unaryPlus() = fromNumerator(numerator)

    override fun toInt() = numerator / precision

    override fun toByte() = toInt().toByte()

    override fun toShort() = toInt().toShort()

    override fun toChar() = toInt().toChar()

    override fun toLong() = toInt().toLong()

    override fun toFloat() = numerator / precision.toFloat()

    override fun toDouble() = numerator / precision.toDouble()

    fun roundToInt() = toDouble().roundToInt()

    /**
     * Determines the square root.
     */
    fun sqrt() =
        fromNumerator(sqrtPrecision * sqrt(numerator.toDouble()).roundToInt())

    /**
     * Determines the absolute value.
     */
    fun abs() =
        fromNumerator(abs(numerator))

    /**
     * Returns the floor of the value.
     */
    fun floor(): Int =
        if (numerator < 0)
            unaryMinus().ceiling().unaryMinus()
        else
            numerator / precision

    /**
     * Returns the ceiling of the value.
     */
    fun ceiling(): Int =
        if (numerator < 0)
            unaryMinus().floor().unaryMinus()
        else
            if (numerator.rem(precision) == 0)
                numerator / precision
            else
                numerator / precision + 1

    override fun hashCode() = numerator

    override fun equals(other: Any?) =
        this === other || (other as? Q)?.numerator == numerator

    override fun toString() =
        roundForPrint(toFloat()).toString()
}

/**
 * Converts the value to a [Q].
 */
fun Number.toQ() =
    Q(this)

/**
 * Determines the square root of [q], equal to [Q.sqrt] on [q].
 */
fun sqrt(q: Q) =
    q.sqrt()

/**
 * Determines the absolute value of [q], equal to [Q.abs] on [q].
 */
fun abs(q: Q) =
    q.abs()


operator fun Q.div(other: Number) =
    div(other.toQ())

operator fun Q.minus(other: Number) =
    minus(other.toQ())

operator fun Q.plus(other: Number) =
    plus(other.toQ())

operator fun Q.times(other: Number) =
    times(other.toQ())

operator fun Number.div(other: Q) =
    toQ().div(other)

operator fun Number.minus(other: Q) =
    toQ().minus(other)

operator fun Number.plus(other: Q) =
    toQ().plus(other)

operator fun Number.times(other: Q) =
    toQ().times(other)