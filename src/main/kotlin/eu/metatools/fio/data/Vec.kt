package eu.metatools.fio.data

import com.badlogic.gdx.math.Vector3
import eu.metatools.up.lang.never
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * A vector.
 */
class Vec(val values: FloatArray, val offset: Int = 0) : Comparable<Vec> {
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

    infix fun cross(other: Vec) =
        Vec(
            y * other.z - z * other.y,
            z * other.x - x * other.z,
            x * other.y - y * other.x
        )

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
    val nor by lazy { if (isEmpty()) Zero else div(len) }

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

    fun lerp(to: Vec, t: Float): Vec {
        val tInv = 1f - t
        return Vec(x * tInv + to.x * t, y * tInv + to.y * t, z * tInv + to.z * t)
    }

    operator fun component1() = x
    operator fun component2() = y
    operator fun component3() = z

    fun isEmpty() = x == 0.0f && y == 0.0f && z == 0.0f

    /**
     * Returns the vector as a [Vector3].
     */
    fun toVector() = Vector3(x, y, z)

    override fun equals(other: Any?) =
        this === other || (other as? Vec)?.let {
            x == it.x && y == it.y && z == it.z
        } ?: false

    override fun hashCode(): Int {
        var r = 31
        r = r * 17 + x.hashCode()
        r = r * 17 + y.hashCode()
        r = r * 17 + z.hashCode()
        return r
    }

    override fun compareTo(other: Vec): Int {
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
        append(roundForPrint(x))
        append(", ")
        append(roundForPrint(y))
        append(", ")
        append(roundForPrint(z))
        append(')')
    }
}

/**
 * True if [Vec] is not [isEmpty]
 */
fun Vec.isNotEmpty() =
    !isEmpty()

/**
 * Returns a [Tri] with the components rounded to int.
 */
fun Vec.roundToInt() =
    Tri(x.roundToInt(), y.roundToInt(), z.roundToInt())


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

fun classify(vec: Vec) =
    if (vec.isEmpty())
        "Zero"
    else
        listOf(
            Vec.X to "X+", -Vec.X to "X-",
            Vec.Y to "Y+", -Vec.Y to "Y-",
            Vec.Z to "Z+", -Vec.Z to "Z-"
        ).maxBy { (v, _) -> vec dot v }?.second ?: never

/**
 * Returns the sequence of integral positions touched around the vector with the given radius.
 */
fun Vec.touching(radius: Float): Sequence<Tri> {
    // Get minimal and maximal components.
    val minX = (x - radius).roundToInt()
    val maxX = (x + radius).roundToInt()
    val minY = (y - radius).roundToInt()
    val maxY = (y + radius).roundToInt()
    val minZ = (z - radius).roundToInt()
    val maxZ = (z + radius).roundToInt()

    // Get ranges.
    val xs = minX..maxX
    val ys = minY..maxY
    val zs = minZ..maxZ

    // Return nested sequence.
    return xs.asSequence().flatMap { x ->
        ys.asSequence().flatMap { y ->
            zs.asSequence().map { z -> Tri(x, y, z) }
        }
    }
}