package eu.metatools.f2d.math

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * TODO: Float is a fuck.
 */
data class Real(val numerator: Int) : Comparable<Real> {

    companion object {
        const val precision = 1024

        const val sqrtPrecision = 32

        val Zero = Real(0)

        val One = Real(precision)

        val MIN_VALUE = Real(Int.MIN_VALUE)

        val MAX_VALUE = Real(Int.MAX_VALUE)
    }

    operator fun dec() =
        Real(numerator - precision)

    operator fun inc() =
        Real(numerator + precision)

    override operator fun compareTo(other: Real) =
        numerator.compareTo(other.numerator)

    operator fun div(other: Real) =
        Real((precision * numerator) / other.numerator)

    operator fun minus(other: Real) =
        Real(numerator - other.numerator)

    operator fun plus(other: Real) =
        Real(numerator + other.numerator)

    operator fun times(other: Real) =
        Real((numerator * other.numerator) / precision)

    operator fun unaryMinus() = Real(-numerator)
    operator fun unaryPlus() = Real(numerator)
    fun toFloat() = numerator / precision.toFloat()
    fun toDouble() = numerator / precision.toDouble()
    override fun toString() = toFloat().toString()
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


fun Int.toReal() =
    Real(this * Real.precision)

fun Float.toReal() =
    Real((this * Real.precision).roundToInt())

fun Double.toReal() =
    Real((this * Real.precision).roundToInt())

fun sqrt(real: Real) = Real(
    Real.sqrtPrecision * sqrt(real.numerator.toDouble()).roundToInt()
)

fun abs(real: Real) =
    Real(abs(real.numerator))

fun min(a: Real, b: Real) =
    if (a < b) a else b

fun max(a: Real, b: Real) =
    if (a < b) b else a

fun hypot(a: Real, b: Real) =
    sqrt(a * a + b * b)

