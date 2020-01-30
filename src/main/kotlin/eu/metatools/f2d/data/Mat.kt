package eu.metatools.f2d.data

import com.badlogic.gdx.math.Matrix4
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Four by four matrix, non-self mutating.
 * @property values The values of the array, provide this with an array that won't be mutated afterwards.
 */
class Mat(val values: FloatArray) {
    /**
     * Creates a new matrix with all zeroes.
     */
    constructor() : this(FloatArray(16))

    /**
     * Creates the matrix from a [Matrix4].
     */
    constructor(matrix4: Matrix4) : this(matrix4.values.clone())

    /**
     * Constructs the array from the given values.
     */
    constructor(
        m00: Float, m01: Float, m02: Float, m03: Float,
        m10: Float, m11: Float, m12: Float, m13: Float,
        m20: Float, m21: Float, m22: Float, m23: Float,
        m30: Float, m31: Float, m32: Float, m33: Float
    ) : this(
        floatArrayOf(
            m00, m10, m20, m30,
            m01, m11, m21, m31,
            m02, m12, m22, m32,
            m03, m13, m23, m33
        )
    )

    companion object {
        /**
         * The identity matrix.
         */
        val ID = Mat(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f
        )

        /**
         * The NaN matrix.
         */
        val NAN = Mat(
            Float.NaN, Float.NaN, Float.NaN, Float.NaN,
            Float.NaN, Float.NaN, Float.NaN, Float.NaN,
            Float.NaN, Float.NaN, Float.NaN, Float.NaN,
            Float.NaN, Float.NaN, Float.NaN, Float.NaN
        )

        /**
         * Creates a translation matrix.
         */
        fun translation(x: Float = 0f, y: Float = 0f, z: Float = 0f) = Mat(
            1f, 0f, 0f, x,
            0f, 1f, 0f, y,
            0f, 0f, 1f, z,
            0f, 0f, 0f, 1f
        )

        /**
         * Creates a translation matrix.
         */
        fun translation(vec: Vec) =
            translation(vec.x, vec.y, vec.z)

        /**
         * Creates a rotation matrix around the axis.
         */
        fun rotation(ax: Float = 0f, ay: Float = 0f, az: Float = 0f, rad: Float = 0f): Mat {
            if (rad == 0f)
                return ID
            if (ax == 0f || ay == 0f || az == 0f)
                return ID

            // Length and normalized axis.
            val d = 1f / hypot(hypot(ax, ay), az)
            val x = ax / d
            val y = ay / d
            val z = az / d

            // Squared values.
            val xx = x * x
            val yy = y * y
            val zz = z * z

            // Trigonometric values.
            val c = cos(rad)
            val oc = 1f - c
            val s = sin(rad)

            // Return result value.
            return Mat(
                c + xx * oc, x * y * oc - z * s, x * z * oc + y * s, 0f,
                y * x * oc + z * s, c + yy * oc, y * z * oc - x * s, 0f,
                z * x * oc - y * s, z * y * oc + x * s, c + zz * oc, 0f,
                0f, 0f, 0f, 1f
            )
        }

        /**
         * Creates a rotation matrix around the axis.
         */
        fun rotation(vec: Vec, rad: Float) =
            rotation(vec.x, vec.y, vec.z, rad)

        /**
         * Creates a rotation matrix around the x-axis.
         */
        fun rotationX(rad: Float): Mat {
            if (rad == 0f)
                return ID

            // Trigonometric values.
            val c = cos(rad)
            val s = sin(rad)

            // Return result value.
            return Mat(
                1f, 0f, 0f, 0f,
                0f, c, -s, 0f,
                0f, s, c, 0f,
                0f, 0f, 0f, 1f
            )
        }

        /**
         * Creates a rotation matrix around the y-axis.
         */
        fun rotationY(rad: Float): Mat {
            if (rad == 0f)
                return ID

            // Trigonometric values.
            val c = cos(rad)
            val s = sin(rad)

            // Return result value.
            return Mat(
                c, 0f, s, 0f,
                0f, 1f, 0f, 0f,
                -s, 0f, c, 0f,
                0f, 0f, 0f, 1f
            )
        }

        /**
         * Creates a rotation matrix around the z-axis.
         */
        fun rotationZ(rad: Float): Mat {
            if (rad == 0f)
                return ID

            // Trigonometric values.
            val c = cos(rad)
            val s = sin(rad)

            // Return result value.
            return Mat(
                c, -s, 0f, 0f,
                s, c, 0f, 0f,
                0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f
            )
        }

        /**
         * Creates a scaling matrix.
         */
        fun scaling(sx: Float = 1f, sy: Float = 1f, sz: Float = 1f) = Mat(
            sx, 0f, 0f, 0f,
            0f, sy, 0f, 0f,
            0f, 0f, sz, 0f,
            0f, 0f, 0f, 1f
        )

        /**
         * Creates a scaling matrix.
         */
        fun scaling(s: Float) = Mat(
            s, 0f, 0f, 0f,
            0f, s, 0f, 0f,
            0f, 0f, s, 0f,
            0f, 0f, 0f, 1f
        )

        /**
         * Creates a scaling matrix.
         */
        fun scaling(vec: Vec) =
            scaling(vec.x, vec.y, vec.z)

        fun ortho2D(x: Float, y: Float, width: Float, height: Float, near: Float, far: Float) =
            ortho(x, x + width, y, y + height, near, far)

        fun ortho(left: Float, right: Float, bottom: Float, top: Float, near: Float, far: Float): Mat {

            val x = 2 / (right - left)
            val y = 2 / (top - bottom)
            val z = -2 / (far - near)

            val tx = -(right + left) / (right - left)
            val ty = -(top + bottom) / (top - bottom)
            val tz = -(far + near) / (far - near)

            return Mat(
                x, 0f, 0f, tx,
                0f, y, 0f, ty,
                0f, 0f, z, tz,
                0f, 0f, 0f, 1f
            )
        }

        /**
         * Creates the matrix from the given axis.
         */
        fun from(x: Vec, y: Vec, z: Vec, pos: Vec = Vec.Zero) =
            Mat(
                x.x, x.y, x.z, pos.x,
                y.x, y.y, y.z, pos.y,
                z.x, z.y, z.z, pos.x,
                0f, 0f, 0f, 1f
            )
    }

    /**
     * Multiplies all vectors.
     */
    operator fun times(vecs: Vecs): Vecs {
        val target = vecs.values.clone()
        Matrix4.mulVec(values, target, 0, target.size / 3, 3)
        return Vecs(*target)
    }

    /**
     * Multiplies all points.
     */
    operator fun times(pts: Pts): Pts {
        val target = toVecValues(pts)
        Matrix4.mulVec(values, target, 0, target.size / 3, 3)
        return fromVecValues(target)
    }

    /**
     * Multiplies all vectors, divides by w-component for projection.
     */
    fun project(vecs: Vecs): Vecs {
        val target = vecs.values.clone()
        Matrix4.prj(values, target, 0, target.size / 3, 3)
        return Vecs(*target)
    }

    /**
     * Multiplies all points, divides by w-component for projection.
     */
    fun project(pts: Pts): Pts {
        val target = toVecValues(pts)
        Matrix4.prj(values, target, 0, target.size / 3, 3)
        return fromVecValues(target)
    }

    /**
     * Multiplies all vectors, ignores translation.
     */
    fun rotate(vecs: Vecs): Vecs {
        val target = vecs.values.clone()
        Matrix4.rot(values, target, 0, target.size / 3, 3)
        return Vecs(*target)
    }

    /**
     * Multiplies all points, ignores translation.
     */
    fun rotate(pts: Pts): Pts {
        val target = toVecValues(pts)
        Matrix4.rot(values, target, 0, target.size / 3, 3)
        return fromVecValues(target)
    }


    /**
     * Multiplies the vector.
     */
    operator fun times(vec: Vec): Vec {
        val target = vec.values.clone()
        Matrix4.mulVec(values, target)
        return Vec(target)
    }

    /**
     * Multiplies the point.
     */
    operator fun times(pt: Pt): Pt {
        val target = floatArrayOf(pt.x, pt.y, 0f)
        Matrix4.mulVec(values, target)
        return Pt(target[0], target[1])
    }

    /**
     * Multiplies the vector, divides by w-component for projection.
     */
    fun project(vec: Vec): Vec {
        val target = vec.values.clone()
        Matrix4.prj(values, target)
        return Vec(target)
    }

    /**
     * Multiplies the point, divides by w-component for projection.
     */
    fun project(pt: Pt): Pt {
        val target = floatArrayOf(pt.x, pt.y, 0f)
        Matrix4.prj(values, target)
        return Pt(target[0], target[1])
    }

    /**
     * Multiplies the vector, ignores translation.
     */
    fun rotate(vec: Vec): Vec {
        val target = vec.values.clone()
        Matrix4.rot(values, target)
        return Vec(target)
    }

    /**
     * Multiplies the point, ignores translation.
     */
    fun rotate(pt: Pt): Pt {
        val target = floatArrayOf(pt.x, pt.y, 0f)
        Matrix4.rot(values, target)
        return Pt(target[0], target[1])
    }

    /**
     * Multiplies the matrices.
     */
    operator fun times(other: Mat): Mat {
        val values = values.clone()
        Matrix4.mul(values, other.values)
        return Mat(values)
    }


    /**
     * Adds a scalar to all values.
     */
    operator fun plus(scalar: Float) = Mat(FloatArray(16) {
        values[it] + scalar
    })

    /**
     * Subtracts a scalar from all values.
     */
    operator fun minus(scalar: Float) = Mat(FloatArray(16) {
        values[it] - scalar
    })

    /**
     * Multiplies all values with a scalar.
     */
    operator fun times(scalar: Float) = Mat(FloatArray(16) {
        values[it] * scalar
    })

    /**
     * Divides all values by a scalar.
     */
    operator fun div(scalar: Float) = Mat(FloatArray(16) {
        values[it] / scalar
    })

    /**
     * Post-multiplies the matrix with the given translation.
     */
    fun translate(x: Float = 0f, y: Float = 0f, z: Float = 0f) =
        times(translation(x, y, z))

    /**
     * Post-multiplies the matrix with the given translation.
     */
    fun translate(vec: Vec) =
        times(translation(vec))

    /**
     * Post-multiplies the matrix with the given rotation.
     */
    fun rotate(ax: Float = 0f, ay: Float = 0f, az: Float = 0f, rad: Float = 0f) =
        times(rotation(ax, ay, az, rad))

    /**
     * Post-multiplies the matrix with the given rotation.
     */
    fun rotate(vec: Vec, rad: Float) =
        times(rotation(vec, rad))

    /**
     * Post-multiplies the matrix with the given rotation.
     */
    fun rotateX(rad: Float) =
        times(rotationX(rad))

    /**
     * Post-multiplies the matrix with the given rotation.
     */
    fun rotateY(rad: Float) =
        times(rotationY(rad))

    /**
     * Post-multiplies the matrix with the given rotation.
     */
    fun rotateZ(rad: Float) =
        times(rotationZ(rad))

    /**
     * Post-multiplies the matrix with the given scaling.
     */
    fun scale(sx: Float = 1f, sy: Float = 1f, sz: Float = 1f) =
        times(scaling(sx, sy, sz))

    /**
     * Post-multiplies the matrix with the given scaling.
     */
    fun scale(s: Float) =
        times(scaling(s))

    /**
     * Post-multiplies the matrix with the given scaling.
     */
    fun scale(vec: Vec) =
        times(scaling(vec))

    /**
     * The inverse of the matrix.
     */
    val inv by lazy {
        val values = values.clone()
        check(Matrix4.inv(values)) { "Matrix not invertible" }
        Mat(values)
    }

    /**
     * The x-vector of the matrix.
     */
    val x by lazy { Vec(values, 0) }

    /**
     * The y-vector of the matrix.
     */
    val y by lazy { Vec(values, 4) }

    /**
     * The z-vector of the matrix.
     */
    val z by lazy { Vec(values, 8) }

    /**
     * The origin of the matrix.
     */
    val center by lazy { Vec(values, 12) }

    /**
     * The length of the longest component.
     */
    val maxLen by lazy { sqrt(maxOf(x.lenSq, maxOf(y.lenSq, z.lenSq))) }

    /**
     * The determinant of the matrix.
     */
    val det by lazy {
        Matrix4.det(values)
    }

    /**
     * Creates a [Matrix4] on a copy of the values.
     */
    fun toMatrix() = Matrix4(values.clone())

    override fun equals(other: Any?) =
        this === other || (other as? Mat)?.values?.contentEquals(values) ?: false

    override fun hashCode() =
        values.contentHashCode()

    override fun toString() = buildString {
        append("{ ")
        append(roundForPrint(values[0]))
        append(", ")
        append(roundForPrint(values[4]))
        append(", ")
        append(roundForPrint(values[8]))
        append(", ")
        append(roundForPrint(values[12]))
        append(", \r\n  ")
        append(roundForPrint(values[1]))
        append(", ")
        append(roundForPrint(values[5]))
        append(", ")
        append(roundForPrint(values[9]))
        append(", ")
        append(roundForPrint(values[3]))
        append(", \r\n  ")
        append(roundForPrint(values[2]))
        append(", ")
        append(roundForPrint(values[6]))
        append(", ")
        append(roundForPrint(values[10]))
        append(", ")
        append(roundForPrint(values[14]))
        append(", \r\n  ")
        append(roundForPrint(values[3]))
        append(", ")
        append(roundForPrint(values[7]))
        append(", ")
        append(roundForPrint(values[13]))
        append(", ")
        append(roundForPrint(values[15]))
        append("}")
    }
}

/**
 * Returns the radians for the degrees.
 */
val Number.deg get() = toFloat() * 0.017453292519943295f
/**
 * Returns the radians for the degrees.
 */
val Float.deg get() = times(0.017453292519943295f)