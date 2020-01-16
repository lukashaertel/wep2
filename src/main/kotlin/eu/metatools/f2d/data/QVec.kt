package eu.metatools.f2d.data

/**
 * A vector.
 */
data class QVec(val x: Q, val y: Q, val z: Q) : Comparable<QVec> {
    companion object {
        /**
         * The x unit vector.
         */
        val X = QVec(Q.ONE, Q.ZERO, Q.ZERO)

        /**
         * The y unit vector.
         */
        val Y = QVec(Q.ZERO, Q.ONE, Q.ZERO)

        /**
         * The z unit vector.
         */
        val Z = QVec(Q.ZERO, Q.ZERO, Q.ONE)

        /**
         * The zero vector.
         */
        val ZERO = QVec(Q.ZERO, Q.ZERO, Q.ZERO)

        /**
         * The one vector.
         */
        val ONE = QVec(Q.ONE, Q.ONE, Q.ONE)

        /**
         * The vector of negative infinity.
         */
        val MIN_VALUE = QVec(Q.MIN_VALUE, Q.MIN_VALUE, Q.MIN_VALUE)

        /**
         * The vector of positive infinity.
         */
        val MAX_VALUE = QVec(Q.MAX_VALUE, Q.MAX_VALUE, Q.MAX_VALUE)
    }

    /**
     * Constructs an empty vector.
     */
    constructor() : this(Q.ZERO, Q.ZERO, Q.ZERO)

    /**
     * Constructs the vector from the given values.
     */
    constructor(x: Number, y: Number, z: Number) : this(Q(x), Q(y), Q(z))

    /**
     * Adds the vector component-wise.
     */
    operator fun plus(other: QVec) = QVec(
        x + other.x,
        y + other.y,
        z + other.z
    )

    /**
     * Subtracts the vector component-wise.
     */
    operator fun minus(other: QVec) = QVec(
        x - other.x,
        y - other.y,
        z - other.z
    )

    /**
     * Multiplies the vector component-wise.
     */
    operator fun times(other: QVec) = QVec(
        x * other.x,
        y * other.y,
        z * other.z
    )

    /**
     * Divides the vector component-wise.
     */
    operator fun div(other: QVec) = QVec(
        x / other.x,
        y / other.y,
        z / other.z
    )

    /**
     * Adds the scalar component-wise.
     */
    operator fun plus(scalar: Q) = QVec(
        x + scalar,
        y + scalar,
        z + scalar
    )


    /**
     * Subtracts the vector component-wise.
     */
    operator fun minus(scalar: Q) = QVec(
        x - scalar,
        y - scalar,
        z - scalar
    )

    /**
     * Multiplies the scalar component-wise.
     */
    operator fun times(scalar: Q) = QVec(
        x * scalar,
        y * scalar,
        z * scalar
    )

    /**
     * Divides the scalar component-wise.
     */
    operator fun div(scalar: Q) = QVec(
        x / scalar,
        y / scalar,
        z / scalar
    )

    /**
     * Negates this vector.
     */
    operator fun unaryMinus() =
        QVec(-x, -y, -z)

    /**
     * The squared length.
     */
    val lenSq by lazy { x * x + y * y + z * z }

    /**
     * The length.
     */
    val len by lazy { sqrt(lenSq) }

    /**
     * The normalized vector.
     */
    val nor by lazy { if (isEmpty()) ZERO else div(len) }

    /**
     * Computes the dot product between this an another vector.
     */
    infix fun dot(other: QVec) =
        x * other.x + y * other.y + z * other.z

    fun isEmpty() = x.numerator == 0 && y.numerator == 0 && z.numerator == 0

    override fun compareTo(other: QVec): Int {
        val rz = z.compareTo(other.z)
        if (rz != 0) return rz
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
        append(", ")
        append(z)
        append(')')
    }
}


fun QVec.isNotEmpty() = !isEmpty()

operator fun QVec.plus(scalar: Int) =
    plus(scalar.toQ())

operator fun QVec.minus(scalar: Int) =
    minus(scalar.toQ())

operator fun QVec.times(scalar: Int) =
    times(scalar.toQ())

operator fun QVec.div(scalar: Int) =
    div(scalar.toQ())

operator fun QVec.plus(scalar: Float) =
    plus(scalar.toQ())

operator fun QVec.minus(scalar: Float) =
    minus(scalar.toQ())

operator fun QVec.times(scalar: Float) =
    times(scalar.toQ())

operator fun QVec.div(scalar: Float) =
    div(scalar.toQ())

operator fun QVec.plus(scalar: Double) =
    plus(scalar.toQ())

operator fun QVec.minus(scalar: Double) =
    minus(scalar.toQ())

operator fun QVec.times(scalar: Double) =
    times(scalar.toQ())

operator fun QVec.div(scalar: Double) =
    div(scalar.toQ())


/**
 * Applies the function on the components.
 */
inline fun QVec.mapComponents(block: (Q) -> Q) =
    QVec(block(x), block(y), block(z))

/**
 * Applies the function on the pairs of components.
 */
inline fun reduceComponents(a: QVec, b: QVec, block: (Q, Q) -> Q) =
    QVec(block(a.x, b.x), block(a.y, b.y), block(a.z, b.z))

/**
 * Returns the componentwise absolute.
 */
fun abs(QVec: QVec) =
    QVec(abs(QVec.x), abs(QVec.y), abs(QVec.z))

/**
 * Converts the [QVec] to a [Vec].
 */
fun QVec.toVec() =
    Vec(x.toFloat(), y.toFloat(), z.toFloat())

/**
 * Converts the [Vec] to a [QVec].
 */
fun Vec.toQVec() =
    QVec(x.toQ(), y.toQ(), z.toQ())
