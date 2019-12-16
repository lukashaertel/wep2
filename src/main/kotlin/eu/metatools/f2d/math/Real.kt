package eu.metatools.f2d.math

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
data class Real(val numerator: Int) : Comparable<Real> {

    companion object {
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
        val ZERO = Real(0)

        /**
         * One.
         */
        val ONE = Real(precision)

        /**
         * Smallest non-zero positive number.
         */
        val E = Real(1)

        /**
         * The minimum value.
         */
        val MIN_VALUE = Real(Int.MIN_VALUE)

        /**
         * The maximum value.
         */
        val MAX_VALUE = Real(Int.MAX_VALUE)
    }

    operator fun dec() =
        Real(numerator - precision)

    operator fun inc() =
        Real(numerator + precision)

    override operator fun compareTo(other: Real) =
        numerator.compareTo(other.numerator)

    operator fun div(other: Real): Real {
        val outNumerator = precision.toLong() * numerator.toLong() / other.numerator.toLong()
        return Real(outNumerator.coerceToInt())
    }


    operator fun minus(other: Real): Real {
        val outNumerator = numerator.toLong() - other.numerator.toLong()
        return Real(outNumerator.coerceToInt())
    }

    operator fun plus(other: Real): Real {
        val outNumerator = numerator.toLong() + other.numerator.toLong()
        return Real(outNumerator.coerceToInt())
    }

    operator fun times(other: Real): Real {
        val outNumerator = (numerator.toLong() * other.numerator.toLong()) / precision.toLong()
        return Real(outNumerator.coerceToInt())
    }

    operator fun unaryMinus() = Real(-numerator)

    operator fun unaryPlus() = Real(numerator)

    fun toFloat() = numerator / precision.toFloat()

    fun toDouble() = numerator / precision.toDouble()

    override fun toString() =
        roundForPrint(toFloat()).toString()
}

operator fun Real.div(other: Int) =
    div(other.toReal())

operator fun Real.minus(other: Int) =
    minus(other.toReal())

operator fun Real.plus(other: Int) =
    plus(other.toReal())

operator fun Real.times(other: Int) =
    times(other.toReal())

operator fun Real.div(other: Float) =
    div(other.toReal())

operator fun Real.minus(other: Float) =
    minus(other.toReal())

operator fun Real.plus(other: Float) =
    plus(other.toReal())

operator fun Real.times(other: Float) =
    times(other.toReal())


operator fun Real.div(other: Double) =
    div(other.toReal())

operator fun Real.minus(other: Double) =
    minus(other.toReal())

operator fun Real.plus(other: Double) =
    plus(other.toReal())

operator fun Real.times(other: Double) =
    times(other.toReal())

/**
 * Converts the value to a [Real].
 */
fun Byte.toReal() =
    Real(this * Real.precision)

/**
 * Converts the value to a [Real].
 */
fun Short.toReal() =
    Real(this * Real.precision)

/**
 * Converts the value to a [Real].
 */
fun Int.toReal() =
    Real(this * Real.precision)

/**
 * Converts the value to a [Real].
 */
fun Long.toReal() =
    Real((this * Real.precision).coerceToInt())

/**
 * Converts the value to a [Real].
 */
fun Float.toReal() =
    Real((this * Real.precision).roundToInt())

/**
 * Converts the value to a [Real].
 */
fun Double.toReal() =
    Real((this * Real.precision).roundToInt())

/**
 * Determines the square root of the value [real].
 */
fun sqrt(real: Real) =
    Real(Real.sqrtPrecision * sqrt(real.numerator.toDouble()).roundToInt())

/**
 * Determines the absolute value of [real].
 */
fun abs(real: Real) =
    Real(abs(real.numerator))

/**
 * Determines the smaller of [a] and [b].
 */
fun min(a: Real, b: Real) =
    if (a < b) a else b

/**
 * Determines the larger of [a] and [b].
 */
fun max(a: Real, b: Real) =
    if (a < b) b else a

/**
 * Determines the hypotenuse of [a] and [b].
 */
fun hypot(a: Real, b: Real) =
    sqrt(a * a + b * b)

