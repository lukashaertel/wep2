package eu.metatools.f2d.data

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Coerces the value of the receiver in the range of valid [Int] values.
 */
private fun Long.coerceToInt() =
    coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()

/**
 * Coerces the value of the receiver in the range of valid [Int] values.
 */
private fun Double.coerceToInt() =
    coerceIn(Int.MIN_VALUE.toDouble(), Int.MAX_VALUE.toDouble()).toInt()

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
         * Returns a [Q] with the specified [numerator]. The fixed [precision] is used as the denominator.
         */
        fun fromNumerator(numerator: Int) =
            Q(Unit, numerator)

        /**
         * Returns a [Q] for the given integer number, shorthand for [from]`(numerator, 1)`
         */
        fun from(numerator: Int) =
            Q(Unit, numerator * precision)

        /**
         * Returns a [Q] for the given fraction.
         */
        fun from(numerator: Int, denominator: Int) =
            Q(Unit, numerator * precision / denominator)

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
        val ZERO = Q(Unit, 0)

        /**
         * One.
         */
        val ONE = from(1)

        /**
         * Two.
         */
        val TWO = from(2)

        /**
         * A quarter.
         */
        val QUARTER = from(1, 4)

        /**
         * A third.
         */
        val THIRD = from(1, 3)

        /**
         * A half.
         */
        val HALF = from(1, 2)

        /**
         * Smallest non-zero positive number.
         */
        val E = Q(Unit, 1)

        /**
         * The minimum value.
         */
        val MIN_VALUE = Q(Unit, Int.MIN_VALUE)

        /**
         * The maximum value.
         */
        val MAX_VALUE = Q(Unit, Int.MAX_VALUE)

    }

    operator fun dec() =
        Q(Unit, numerator - precision)

    operator fun inc() =
        Q(Unit, numerator + precision)

    override operator fun compareTo(other: Q) =
        numerator.compareTo(other.numerator)

    operator fun div(other: Q): Q {
        val outNumerator = precision.toLong() * numerator.toLong() / other.numerator.toLong()
        return Q(Unit, outNumerator.coerceToInt())
    }

    operator fun minus(other: Q): Q {
        val outNumerator = numerator.toLong() - other.numerator.toLong()
        return Q(Unit, outNumerator.coerceToInt())
    }

    operator fun plus(other: Q): Q {
        val outNumerator = numerator.toLong() + other.numerator.toLong()
        return Q(Unit, outNumerator.coerceToInt())
    }

    operator fun times(other: Q): Q {
        val outNumerator = (numerator.toLong() * other.numerator.toLong()) / precision.toLong()
        return Q(Unit, outNumerator.coerceToInt())
    }

    operator fun div(other: Number) =
        div(other.toQ())

    operator fun minus(other: Number) =
        minus(other.toQ())

    operator fun plus(other: Number) =
        plus(other.toQ())

    operator fun times(other: Number) =
        times(other.toQ())


    operator fun unaryMinus() = Q(Unit, -numerator)

    operator fun unaryPlus() = Q(Unit, numerator)

    override fun toInt() = numerator / precision

    override fun toByte() = toInt().toByte()

    override fun toShort() = toInt().toShort()

    override fun toChar() = toInt().toChar()

    override fun toLong() = toInt().toLong()

    override fun toFloat() = numerator / precision.toFloat()

    override fun toDouble() = numerator / precision.toDouble()

    /**
     * Rounds the value. The round-half-up method is applied, compatible to JVM internal rounding.
     */
    fun roundToInt() =
        plus(HALF).floor()

    /**
     * Determines the square root.
     */
    fun sqrt() =
        Q(Unit, sqrtPrecision * sqrt(numerator.toDouble()).roundToInt())

    /**
     * Raises this value to the [e]th power.
     */
    fun pow(e: Int): Q {
        val outNumerator = precision.toDouble() * numerator.toDouble().pow(e) / precision.toDouble().pow(e)
        return Q(Unit, outNumerator.coerceToInt())
    }

    /**
     * Returns the reciprocal of the value.
     */
    fun rcp(): Q {
        val outNumerator = precision.toLong() * precision.toLong() / numerator.toLong()
        return Q(Unit, outNumerator.coerceToInt())
    }

    /**
     * Determines the absolute value.
     */
    fun abs() =
        Q(Unit, abs(numerator))

    /**
     * Returns the floor of the value.
     */
    fun floor(): Int =
        numerator / precision + numerator.rem(precision).coerceIn(-1, 0)

    /**
     * Returns the ceiling of the value.
     */
    fun ceiling(): Int =
        numerator / precision + numerator.rem(precision).coerceIn(0, 1)

    override fun hashCode() = numerator

    override fun equals(other: Any?) =
        this === other || (other as? Q)?.numerator == numerator

    override fun toString() =
        roundForPrint(toFloat()).toString()
}

infix fun Int.over(denominator: Int) =
    Q.from(this, denominator)

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
 * Computes the hypotenuse of [x] and [y].
 */
fun hypot(x: Q, y: Q) =
    (x * x + y * y).sqrt()
/**
 * Determines the absolute value of [q], equal to [Q.abs] on [q].
 */
fun abs(q: Q) =
    q.abs()

operator fun Number.div(other: Q) =
    toQ().div(other)

operator fun Number.minus(other: Q) =
    toQ().minus(other)

operator fun Number.plus(other: Q) =
    toQ().plus(other)

operator fun Number.times(other: Q) =
    toQ().times(other)
