package eu.metatools.f2d.math

import com.badlogic.gdx.math.Vector2
import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * A point.
 */
class Pt(val values: FloatArray, val offset: Int = 0) : Externalizable {
    companion object {
        /**
         * The x unit point.
         */
        val X = Pt(1f, 0f)

        /**
         * The y unit point.
         */
        val Y = Pt(0f, 1f)

        /**
         * The zero point.
         */
        val Zero = Pt(0f, 0f)

        /**
         * The one point.
         */
        val One = Pt(1f, 1f)

        /**
         * The point of NaN values.
         */
        val NaN = Pt(Float.NaN, Float.NaN)

        /**
         * The point of negative infinity.
         */
        val NegativeInfinity = Pt(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY)

        /**
         * The point of positive infinity.
         */
        val PositiveInfinity = Pt(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    }

    /**
     * Constructs an empty point on a new backing array.
     */
    constructor() : this(FloatArray(2), 0)

    /**
     * Constructs the point from a [Vector2].
     */
    constructor(vector2: Vector2) : this(floatArrayOf(vector2.x, vector2.y))

    /**
     * Constructs the point from the given values.
     */
    constructor(x: Float, y: Float) : this(floatArrayOf(x, y))

    /**
     * Adds the point component-wise.
     */
    operator fun plus(other: Pt) = Pt(
        x + other.x,
        y + other.y
    )

    /**
     * Subtracts the point component-wise.
     */
    operator fun minus(other: Pt) = Pt(
        x - other.x,
        y - other.y
    )

    /**
     * Multiplies the point component-wise.
     */
    operator fun times(other: Pt) = Pt(
        x * other.x,
        y * other.y
    )

    /**
     * Divides the point component-wise.
     */
    operator fun div(other: Pt) = Pt(
        x / other.x,
        y / other.y
    )

    /**
     * Adds the scalar component-wise.
     */
    operator fun plus(scalar: Float) = Pt(
        x + scalar,
        y + scalar
    )


    /**
     * Subtracts the point component-wise.
     */
    operator fun minus(scalar: Float) = Pt(
        x - scalar,
        y - scalar
    )

    /**
     * Multiplies the scalar component-wise.
     */
    operator fun times(scalar: Float) = Pt(
        x * scalar,
        y * scalar
    )

    /**
     * Divides the scalar component-wise.
     */
    operator fun div(scalar: Float) = Pt(
        x / scalar,
        y / scalar
    )

    /**
     * Negates this point.
     */
    operator fun unaryMinus() =
        Pt(-x, -y)

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
    infix fun dot(other: Pt) =
        x * other.x + y * other.y

    /**
     * Gets the component as `[x, y]`
     */
    operator fun get(n: Int) = values[offset + maxOf(0, minOf(n, 1))]

    /**
     * The x-component.
     */
    val x get() = values[offset + 0]

    /**
     * The y-component.
     */
    val y get() = values[offset + 1]

    operator fun component1() = x
    operator fun component2() = y

    val isEmpty get() = x == 0.0f && y == 0.0f

    /**
     * Returns the point as a [Vector2].
     */
    fun toVector() = Vector2(x, y)

    override fun equals(other: Any?) =
        this === other || (other as? Pt)?.let {
            x == it.x && y == it.y
        } ?: false

    override fun hashCode(): Int {
        var r = 31
        r = r * 17 + x.hashCode()
        r = r * 17 + y.hashCode()
        return r
    }


    override fun toString() = buildString {
        append('(')
        append(roundForPrint(x))
        append(", ")
        append(roundForPrint(y))
        append(')')
    }

    override fun readExternal(input: ObjectInput) {
        values[0] = input.readFloat()
        values[1] = input.readFloat()
    }

    override fun writeExternal(output: ObjectOutput) {
        output.writeFloat(x)
        output.writeFloat(y)
    }
}

/**
 * Applies the function on the components.
 */
inline fun Pt.mapComponents(block: (Float) -> Float) =
    Pt(block(x), block(y))

/**
 * Applies the function on the pairs of components.
 */
inline fun reduceComponents(a: Pt, b: Pt, block: (Float, Float) -> Float) =
    Pt(block(a.x, b.x), block(a.y, b.y))

/**
 * Returns the componentwise absolute.
 */
fun abs(pt: Pt) =
    Pt(abs(pt.x), abs(pt.y))