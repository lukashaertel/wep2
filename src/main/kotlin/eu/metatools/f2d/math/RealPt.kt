package eu.metatools.f2d.math

data class RealPt(val x: Real, val y: Real) {
    companion object {
        /**
         * The x unit point.
         */
        val X = RealPt(
            Real.One,
            Real.Zero
        )

        /**
         * The y unit point.
         */
        val Y = RealPt(
            Real.Zero,
            Real.One
        )

        /**
         * The zero point.
         */
        val Zero = RealPt(
            Real.Zero,
            Real.Zero
        )

        /**
         * The one point.
         */
        val One = RealPt(
            Real.One,
            Real.One
        )

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