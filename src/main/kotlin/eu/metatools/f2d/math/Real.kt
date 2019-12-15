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

fun Int.toReal() =
    Real(this * Real.precision)

fun Float.toReal() =
    Real((this * Real.precision).roundToInt())

fun Double.toReal() =
    Real((this * Real.precision).roundToInt())

fun sqrt(real: Real) = Real(
    Real.sqrtPrecision * sqrt(real.numerator.toDouble()).roundToInt()
)

data class RealPt(val x: Real, val y: Real) {
    companion object {
        /**
         * The x unit point.
         */
        val X = RealPt(Real.One, Real.Zero)

        /**
         * The y unit point.
         */
        val Y = RealPt(Real.Zero, Real.One)

        /**
         * The zero point.
         */
        val Zero = RealPt(Real.Zero, Real.Zero)

        /**
         * The one point.
         */
        val One = RealPt(Real.One, Real.One)

    }

    constructor() : this(
        Real.Zero,
        Real.Zero
    )

    /**
     * Adds the point component-wise.
     */
    operator fun plus(other: RealPt) =
        RealPt(
            x + other.x,
            y + other.y
        )

    /**
     * Subtracts the point component-wise.
     */
    operator fun minus(other: RealPt) =
        RealPt(
            x - other.x,
            y - other.y
        )

    /**
     * Multiplies the point component-wise.
     */
    operator fun times(other: RealPt) =
        RealPt(
            x * other.x,
            y * other.y
        )

    /**
     * Divides the point component-wise.
     */
    operator fun div(other: RealPt) =
        RealPt(
            x / other.x,
            y / other.y
        )

    /**
     * Adds the scalar component-wise.
     */
    operator fun plus(scalar: Real) =
        RealPt(
            x + scalar,
            y + scalar
        )


    /**
     * Subtracts the point component-wise.
     */
    operator fun minus(scalar: Real) =
        RealPt(
            x - scalar,
            y - scalar
        )

    /**
     * Multiplies the scalar component-wise.
     */
    operator fun times(scalar: Real) =
        RealPt(
            x * scalar,
            y * scalar
        )

    /**
     * Divides the scalar component-wise.
     */
    operator fun div(scalar: Real) =
        RealPt(
            x / scalar,
            y / scalar
        )

    /**
     * Negates this point.
     */
    operator fun unaryMinus() =
        RealPt(-x, -y)

    /**
     * The squared length.
     */
    val lenSq by lazy { x * x + y * y }

    /**
     * The length.
     */
    val len by lazy { sqrt(lenSq) }

    /**
     * The normalized point.
     */
    val nor by lazy { div(len) }

    /**
     * Computes the dot product between this an another point.
     */
    infix fun dot(other: RealPt) =
        x * other.x + y * other.y

    val isEmpty get() = x.numerator == 0 && y.numerator == 0
}

/**
 * Applies the function on the components.
 */
inline fun RealPt.mapComponents(block: (Real) -> Real) =
    RealPt(block(x), block(y))

/**
 * Applies the function on the pairs of components.
 */
inline fun reduceComponents(a: RealPt, b: RealPt, block: (Real, Real) -> Real) =
    RealPt(block(a.x, b.x), block(a.y, b.y))

fun abs(real: Real) =
    Real(abs(real.numerator))

fun abs(pt: RealPt) =
    RealPt(abs(pt.x), abs(pt.y))

fun min(a: Real, b: Real) =
    if (a < b) a else b

fun max(a: Real, b: Real) =
    if (a < b) b else a

fun hypot(a: Real, b: Real) =
    sqrt(a * a + b * b)

fun RealPt.toPt() = Pt(x.toFloat(), y.toFloat())
fun Pt.toReal() = RealPt(x.toReal(), y.toReal())