package eu.metatools.f2d.math

import com.badlogic.gdx.math.Vector3
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * A vector.
 */
class Vec(val values: FloatArray) {
    companion object {
        /**
         * The x unit vector.
         */
        val x = Vec(1f, 0f, 0f)

        /**
         * The y unit vector.
         */
        val y = Vec(0f, 1f, 0f)

        /**
         * The z unit vector.
         */
        val z = Vec(0f, 0f, 1f)

        /**
         * The zero vector.
         */
        val zero = Vec(0f, 0f, 0f)

        /**
         * The one vector.
         */
        val one = Vec(1f, 1f, 1f)
    }

    /**
     * Constructs the vector from a [Vector3].
     */
    constructor(vector3: Vector3) : this(floatArrayOf(vector3.x, vector3.y, vector3.z))

    /**
     * Constructs the vector from the given values.
     */
    constructor(x: Float, y: Float, z: Float) : this(floatArrayOf(x, y, z))

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
    val lenSq by lazy { hypot(hypot(x, y), z) }

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
    operator fun get(n: Int) = values[n]

    /**
     * The x-component.
     */
    val x get() = values[0]

    /**
     * The y-component.
     */
    val y get() = values[1]

    /**
     * The z-component.
     */
    val z get() = values[2]

    operator fun component1() = x
    operator fun component2() = y
    operator fun component3() = z

    /**
     * Returns the vector as a [Vector3].
     */
    fun toVector() = Vector3(values.clone())

    override fun equals(other: Any?) =
        this === other || (other as? Vec)?.values?.contentEquals(values) ?: false

    override fun hashCode() =
        values.contentHashCode()

    override fun toString() = buildString {
        append('(')
        append(roundForPrint(values[0]))
        append(", ")
        append(roundForPrint(values[1]))
        append(", ")
        append(roundForPrint(values[2]))
        append(')')
    }
}