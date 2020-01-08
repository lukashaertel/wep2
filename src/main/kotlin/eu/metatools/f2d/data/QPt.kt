package eu.metatools.f2d.data

import kotlin.math.atan2

/**
 * A point of [Q]s.
 */
data class QPt(val x: Q, val y: Q) : Comparable<QPt> {
    companion object {
        /**
         * The x unit point.
         */
        val X = QPt(Q.ONE, Q.ZERO)

        /**
         * The y unit point.
         */
        val Y = QPt(Q.ZERO, Q.ONE)

        /**
         * The zero point.
         */
        val ZERO = QPt(Q.ZERO, Q.ZERO)

        /**
         * The one point.
         */
        val ONE = QPt(Q.ONE, Q.ONE)

        /**
         * The point of minimum real value.
         */
        val MIN_VALUE = QPt(Q.MIN_VALUE, Q.MIN_VALUE)

        /**
         * The point of maximum real value.
         */
        val MAX_VALUE = QPt(Q.MAX_VALUE, Q.MAX_VALUE)

    }

    /**
     * Creates a zero real point.
     */
    constructor() : this(Q.ZERO, Q.ZERO)

    /**
     * Constructs a new [QPt] with the arguments converted via [toQ].
     */
    constructor(x: Number, y: Number) : this(x.toQ(), y.toQ())


    /**
     * Adds the point component-wise.
     */
    operator fun plus(other: QPt) =
        QPt(
            x + other.x,
            y + other.y
        )

    /**
     * Subtracts the point component-wise.
     */
    operator fun minus(other: QPt) =
        QPt(
            x - other.x,
            y - other.y
        )

    /**
     * Multiplies the point component-wise.
     */
    operator fun times(other: QPt) =
        QPt(
            x * other.x,
            y * other.y
        )

    /**
     * Divides the point component-wise.
     */
    operator fun div(other: QPt) =
        QPt(
            x / other.x,
            y / other.y
        )

    /**
     * Adds the scalar component-wise.
     */
    operator fun plus(scalar: Q) =
        QPt(
            x + scalar,
            y + scalar
        )


    /**
     * Subtracts the point component-wise.
     */
    operator fun minus(scalar: Q) =
        QPt(
            x - scalar,
            y - scalar
        )

    /**
     * Multiplies the scalar component-wise.
     */
    operator fun times(scalar: Q) =
        QPt(
            x * scalar,
            y * scalar
        )

    /**
     * Divides the scalar component-wise.
     */
    operator fun div(scalar: Q) =
        QPt(
            x / scalar,
            y / scalar
        )

    /**
     * Negates this point.
     */
    operator fun unaryMinus() =
        QPt(-x, -y)

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
     * The angel, computed via [atan2].
     */
    val angle by lazy { atan2(y.toDouble(), x.toDouble()).toQ() }

    /**
     * Computes the dot product between this an another point.
     */
    infix fun dot(other: QPt) =
        x * other.x + y * other.y

    fun isEmpty() = x.numerator == 0 && y.numerator == 0

    override fun compareTo(other: QPt): Int {
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

fun QPt.isNotEmpty() = !isEmpty()

operator fun QPt.plus(scalar: Int) =
    plus(scalar.toQ())

operator fun QPt.minus(scalar: Int) =
    minus(scalar.toQ())

operator fun QPt.times(scalar: Int) =
    times(scalar.toQ())

operator fun QPt.div(scalar: Int) =
    div(scalar.toQ())

operator fun QPt.plus(scalar: Float) =
    plus(scalar.toQ())

operator fun QPt.minus(scalar: Float) =
    minus(scalar.toQ())

operator fun QPt.times(scalar: Float) =
    times(scalar.toQ())

operator fun QPt.div(scalar: Float) =
    div(scalar.toQ())

operator fun QPt.plus(scalar: Double) =
    plus(scalar.toQ())

operator fun QPt.minus(scalar: Double) =
    minus(scalar.toQ())

operator fun QPt.times(scalar: Double) =
    times(scalar.toQ())

operator fun QPt.div(scalar: Double) =
    div(scalar.toQ())


/**
 * Applies the function on the components.
 */
inline fun QPt.mapComponents(block: (Q) -> Q) =
    QPt(block(x), block(y))

/**
 * Applies the function on the pairs of components.
 */
inline fun reduceComponents(a: QPt, b: QPt, block: (Q, Q) -> Q) =
    QPt(block(a.x, b.x), block(a.y, b.y))

fun abs(pt: QPt) =
    QPt(
        abs(pt.x),
        abs(pt.y)
    )

fun QPt.toPt() =
    Pt(x.toFloat(), y.toFloat())

fun Pt.toQ() =
    QPt(x.toQ(), y.toQ())

/**
 * Sums up all values.
 */
fun Iterable<QPt>.sum() = fold(0 to 0) { (x, y), it ->
    // Sum up both parts as integers.
    (x + it.x.numerator) to (y + it.y.numerator)
}.let { (x, y) ->
    // Convert to real point.
    QPt(Q(x), Q(y))
}

/**
 * Gets the average of the iterable.
 */
fun Iterable<QPt>.avg() = fold(Triple(0, 0, 0)) { (x, y, c), it ->
    // Sum up both parts as integers.
    Triple(x + it.x.numerator, y + it.y.numerator, c.inc())
}.let { (x, y, c) ->
    // Convert to real point.
    QPt(Q(x / c), Q(y / c))
}

/**
 * Sums up all values.
 */
fun Sequence<QPt>.sum() = fold(0 to 0) { (x, y), it ->
    // Sum up both parts as integers.
    (x + it.x.numerator) to (y + it.y.numerator)
}.let { (x, y) ->
    // Convert to real point.
    QPt(Q(x), Q(y))
}