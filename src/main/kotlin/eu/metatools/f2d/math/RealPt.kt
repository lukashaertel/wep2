package eu.metatools.f2d.math

/**
 * A point of [Real]s.
 */
data class RealPt(val x: Real, val y: Real) : Comparable<RealPt> {
    companion object {
        /**
         * The x unit point.
         */
        val X = RealPt(Real.ONE, Real.ZERO)

        /**
         * The y unit point.
         */
        val Y = RealPt(Real.ZERO, Real.ONE)

        /**
         * The zero point.
         */
        val ZERO = RealPt(Real.ZERO, Real.ZERO)

        /**
         * The one point.
         */
        val ONE = RealPt(Real.ONE, Real.ONE)

        /**
         * The point of minimum real value.
         */
        val MIN_VALUE = RealPt(Real.MIN_VALUE, Real.MIN_VALUE)

        /**
         * The point of maximum real value.
         */
        val MAX_VALUE = RealPt(Real.MAX_VALUE, Real.MAX_VALUE)

        /**
         * Constructs a new [RealPt] with the arguments converted via [toReal].
         */
        fun from(x: Int, y: Int) =
            RealPt(x.toReal(), y.toReal())

        /**
         * Constructs a new [RealPt] with the arguments converted via [toReal].
         */
        fun from(x: Float, y: Float) =
            RealPt(x.toReal(), y.toReal())

        /**
         * Constructs a new [RealPt] with the arguments converted via [toReal].
         */
        fun from(x: Double, y: Double) =
            RealPt(x.toReal(), y.toReal())
    }

    /**
     * Creates a zero real point.
     */
    constructor() : this(
        Real.ZERO,
        Real.ZERO
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

    override fun compareTo(other: RealPt): Int {
        val ry = y.compareTo(other.y)
        if (ry != 0) return ry
        val rx = x.compareTo(other.x)
        if (rx != 0) return rx
        return 0
    }

    override fun toString() = buildString {
        append('(')
        append(x)
        append(", ")
        append(y)
        append(')')
    }
}

operator fun RealPt.plus(scalar: Int) =
    plus(scalar.toReal())

operator fun RealPt.minus(scalar: Int) =
    minus(scalar.toReal())

operator fun RealPt.times(scalar: Int) =
    times(scalar.toReal())

operator fun RealPt.div(scalar: Int) =
    div(scalar.toReal())

operator fun RealPt.plus(scalar: Float) =
    plus(scalar.toReal())

operator fun RealPt.minus(scalar: Float) =
    minus(scalar.toReal())

operator fun RealPt.times(scalar: Float) =
    times(scalar.toReal())

operator fun RealPt.div(scalar: Float) =
    div(scalar.toReal())

operator fun RealPt.plus(scalar: Double) =
    plus(scalar.toReal())

operator fun RealPt.minus(scalar: Double) =
    minus(scalar.toReal())

operator fun RealPt.times(scalar: Double) =
    times(scalar.toReal())

operator fun RealPt.div(scalar: Double) =
    div(scalar.toReal())


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

fun abs(pt: RealPt) =
    RealPt(
        abs(pt.x),
        abs(pt.y)
    )

fun RealPt.toPt() =
    Pt(x.toFloat(), y.toFloat())

fun Pt.toReal() =
    RealPt(x.toReal(), y.toReal())