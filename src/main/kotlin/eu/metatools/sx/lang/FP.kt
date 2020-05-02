package eu.metatools.sx.lang

/**
 * Resolution of [FP].
 */
private const val fpValueZero: Short = 0

/**
 * Resolution of [FP].
 */
private const val fpOne: Short = 100

/**
 * Resolution of [FP] as a float.
 */
private const val fpOneFloat = fpOne.toFloat()

/**
 * Resolution of [FP] as a double.
 */
private const val fpOneDouble = fpOne.toDouble()

/**
 * [FP.value] primary type.
 */
private typealias FPValue = Short

/**
 * Rational over fixed precision.
 */
inline class FP(val value: FPValue) {
    companion object {
        val zero = 0.fp
        val one = 1.fp
    }

    constructor(intValue: Int) : this(intValue.toShort())

    operator fun plus(other: FP) =
        FP(value + other.value)

    operator fun minus(other: FP) =
        FP(value - other.value)

    operator fun times(other: FP) =
        FP(value * other.value / fpOne)

    operator fun div(other: FP) =
        FP(fpOne * value / other.value)

    operator fun unaryMinus() =
        FP(-value)

    operator fun unaryPlus() =
        FP(value)

    operator fun dec() =
        FP(value - fpOne)

    operator fun inc() =
        FP(value + fpOne)

    fun toInt() =
        value / fpOne

    fun toFloat() =
        value.toFloat() / fpOneFloat

    fun toDouble() =
        value.toDouble() / fpOneDouble

    operator fun rangeTo(other: FP) =
        toFloat()..other.toFloat()

    operator fun compareTo(other: FP) =
        value.compareTo(other.value)

    fun isZero() =
        value == fpValueZero

    fun isNotZero() =
        value != fpValueZero

    override fun toString(): String {
        return toFloat().toString()
    }
}

operator fun FP.plus(other: Int) =
    plus(other.fp)

operator fun FP.minus(other: Int) =
    minus(other.fp)

operator fun FP.times(other: Int) =
    times(other.fp)

operator fun FP.div(other: Int) =
    div(other.fp)

operator fun FP.plus(other: Float) =
    plus(other.fp)

operator fun FP.minus(other: Float) =
    minus(other.fp)

operator fun FP.times(other: Float) =
    times(other.fp)

operator fun FP.div(other: Float) =
    div(other.fp)

operator fun FP.plus(other: Double) =
    plus(other.fp)

operator fun FP.minus(other: Double) =
    minus(other.fp)

operator fun FP.times(other: Double) =
    times(other.fp)

operator fun FP.div(other: Double) =
    div(other.fp)

fun FP.coerceAtLeast(minimumValue: FP) =
    FP(value.coerceAtLeast(minimumValue.value))

fun FP.coerceAtMost(maximumValue: FP) =
    FP(value.coerceAtMost(maximumValue.value))

fun FP.coerceIn(minimumValue: FP, maximumValue: FP) =
    FP(value.coerceIn(minimumValue.value, maximumValue.value))

val Byte.fp get() = FP(this * fpOne)
val Short.fp get() = FP(this * fpOne)
val Int.fp get() = FP(this * fpOne)
val Long.fp get() = FP((this * fpOne).toInt())
val Float.fp get() = FP((this * fpOneFloat).toInt())
val Double.fp get() = FP((this * fpOneDouble).toInt())
