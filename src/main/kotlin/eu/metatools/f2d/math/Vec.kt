package eu.metatools.f2d.math

import com.badlogic.gdx.math.Vector3
import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * A vector.
 */
class Vec(val values: FloatArray, val offset: Int = 0) : Externalizable {
    companion object {
        /**
         * The x unit vector.
         */
        val X = Vec(1f, 0f, 0f)

        /**
         * The y unit vector.
         */
        val Y = Vec(0f, 1f, 0f)

        /**
         * The z unit vector.
         */
        val Z = Vec(0f, 0f, 1f)

        /**
         * The zero vector.
         */
        val Zero = Vec(0f, 0f, 0f)

        /**
         * The one vector.
         */
        val One = Vec(1f, 1f, 1f)

        /**
         * The vector of NaN values.
         */
        val NaN = Vec(Float.NaN, Float.NaN, Float.NaN)

        /**
         * The vector of negative infinity.
         */
        val NegativeInfinity = Vec(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY)

        /**
         * The vector of positive infinity.
         */
        val PositiveInfinity = Vec(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    }

    /**
     * Constructs an empty vector on a new backing array.
     */
    constructor() : this(FloatArray(3), 0)

    /**
     * Constructs the vector from a [Vector3].
     */
    constructor(vector3: Vector3) : this(floatArrayOf(vector3.x, vector3.y, vector3.z))

    /**
     * Constructs the vector from the given values.
     */
    constructor(x: Float = 0f, y: Float = 0f, z: Float = 0f) : this(floatArrayOf(x, y, z))

    /**
     * Adds the vector component-wise.
     */
    operator fun plus(other: Vec) = Vec(
        x + other.x,
        y + other.y,
        z + other.z
    )

    /**
     * Subtracts the vector component-wise.
     */
    operator fun minus(other: Vec) = Vec(
        x - other.x,
        y - other.y,
        z - other.z
    )

    /**
     * Multiplies the vector component-wise.
     */
    operator fun times(other: Vec) = Vec(
        x * other.x,
        y * other.y,
        z * other.z
    )

    /**
     * Divides the vector component-wise.
     */
    operator fun div(other: Vec) = Vec(
        x / other.x,
        y / other.y,
        z / other.z
    )

    /**
     * Adds the scalar component-wise.
     */
    operator fun plus(scalar: Float) = Vec(
        x + scalar,
        y + scalar,
        z + scalar
    )


    /**
     * Subtracts the vector component-wise.
     */
    operator fun minus(scalar: Float) = Vec(
        x - scalar,
        y - scalar,
        z - scalar
    )

    /**
     * Multiplies the scalar component-wise.
     */
    operator fun times(scalar: Float) = Vec(
        x * scalar,
        y * scalar,
        z * scalar
    )

    /**
     * Divides the scalar component-wise.
     */
    operator fun div(scalar: Float) = Vec(
        x / scalar,
        y / scalar,
        z / scalar
    )

    /**
     * Negates this vector.
     */
    operator fun unaryMinus() =
        Vec(-x, -y, -z)

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
    val nor by lazy { div(len) }

    /**
     * Computes the dot product between this an another vector.
     */
    infix fun dot(other: Vec) =
        x * other.x + y * other.y + z * other.z

    /**
     * Gets the component as `[x, y, z]`
     */
    operator fun get(n: Int) = values[offset + maxOf(0, minOf(n, 2))]

    /**
     * The x-component.
     */
    val x get() = values[offset + 0]

    /**
     * The y-component.
     */
    val y get() = values[offset + 1]

    /**
     * The z-component.
     */
    val z get() = values[offset + 2]

    operator fun component1() = x
    operator fun component2() = y
    operator fun component3() = z

    val isEmpty get() = x == 0.0f && y == 0.0f && z == 0.0f

    /**
     * Returns the vector as a [Vector3].
     */
    fun toVector() = Vector3(x, y, z)

    override fun equals(other: Any?) =
        this === other || (other as? Vec)?.let {
            x == it.x && y == it.x && z == it.z
        } ?: false

    override fun hashCode(): Int {
        var r = 31
        r = r * 17 + x.hashCode()
        r = r * 17 + y.hashCode()
        r = r * 17 + z.hashCode()
        return r
    }


    override fun toString() = buildString {
        append('(')
        append(roundForPrint(x))
        append(", ")
        append(roundForPrint(y))
        append(", ")
        append(roundForPrint(z))
        append(')')
    }

    override fun readExternal(input: ObjectInput) {
        values[0] = input.readFloat()
        values[1] = input.readFloat()
        values[2] = input.readFloat()
    }

    override fun writeExternal(output: ObjectOutput) {
        output.writeFloat(x)
        output.writeFloat(y)
        output.writeFloat(z)
    }
}

/**
 * Applies the function on the components.
 */
inline fun Vec.mapComponents(block: (Float) -> Float) =
    Vec(block(x), block(y), block(z))

/**
 * Applies the function on the pairs of components.
 */
inline fun reduceComponents(a: Vec, b: Vec, block: (Float, Float) -> Float) =
    Vec(block(a.x, b.x), block(a.y, b.y), block(a.z, b.z))

/**
 * Returns the componentwise absolute.
 */
fun abs(vec: Vec) =
    Vec(abs(vec.x), abs(vec.y), abs(vec.z))