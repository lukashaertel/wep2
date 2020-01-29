package eu.metatools.ex.data

import eu.metatools.ex.math.td
import eu.metatools.f2d.data.Vec
import eu.metatools.f2d.data.Vecs
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * A pair of points and triangle indices.
 * @property points The points of the mesh.
 * @param indices The indices of the mesh.
 */
data class Mesh(val points: List<Vec>, val indices: IntArray) {
    companion object {
        /**
         * An empty mesh.
         */
        val NONE = Mesh(emptyList(), intArrayOf())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Mesh

        if (points != other.points) return false
        if (!indices.contentEquals(other.indices)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = points.hashCode()
        result = 31 * result + indices.contentHashCode()
        return result
    }

    /**
     * Returns a mesh that includes both this and the [other] mesh.
     */
    infix fun union(other: Mesh): Mesh {
        val points = points + other.points
        val indices = IntArray(indices.size + other.indices.size) {
            if (it < indices.size)
                indices[it]
            else
                other.indices[it - indices.size] + points.size
        }
        return Mesh(points, indices)
    }
}

/**
 * Iterates all triangles.
 */
inline fun Mesh.forEach(triBlock: (Vec, Vec, Vec) -> Unit) {
    var i = 0
    while (i < indices.size) {
        triBlock(points[indices[i]], points[indices[i + 1]], points[indices[i + 2]])
        i += 3
    }
}

/**
 * Returns the triangle with the smallest absolute distance to the point [p].
 */
fun Mesh.closest(p: Vec): Pair<Vecs, Float> {
    // Tracked minimal values.
    var minimal = Float.MAX_VALUE
    val result = Vecs(3)

    // Iterate all triangles, underwrite if closer.
    forEach { v1, v2, v3 ->
        val new = td(p, v1, v2, v3)
        if (new < minimal) {
            minimal = new
            result.values[0] = v1.x
            result.values[1] = v1.y
            result.values[2] = v1.z
            result.values[3] = v2.x
            result.values[4] = v2.y
            result.values[5] = v2.z
            result.values[6] = v3.x
            result.values[7] = v3.y
            result.values[8] = v3.z
        }
    }

    // Return the minimized vectors with the distance.
    return result to sqrt(minimal)
}

/**
 * Returns true if the point is inside the mesh. The mesh must be convex.
 */
fun Mesh.inside(p: Vec, radius: Float): Boolean {
    // If above any triangle, return false.
    forEach { v1, v2, v3 ->
        val n = (v3 - v1 cross v2 - v1).nor
        val d = (p - v1) dot n

        if (d >= radius)
            return false
    }

    // Return true, under all triangles.
    return true
}

/**
 * For a four indices, clockwise quad definition, returns six indices representing the constituting triangles.
 */
fun quad(a: Int, b: Int, c: Int, d: Int) =
    intArrayOf(a, b, c, c, d, a)

/**
 * Returns the box around coordinates ([x], [y], [z]) with the given or the default dimensions.
 */
fun box(
    x: Float, y: Float, z: Float,
    xn: Float = -0.5f, xp: Float = 0.5f,
    yn: Float = -0.5f, yp: Float = 0.5f,
    zn: Float = -0.5f, zp: Float = 0.5f
): Mesh {
    val points = listOf(
        Vec(x + xn, y + yn, z + zn),
        Vec(x + xn, y + yn, z + zp),
        Vec(x + xn, y + yp, z + zn),
        Vec(x + xn, y + yp, z + zp),

        Vec(x + xp, y + yn, z + zn),
        Vec(x + xp, y + yn, z + zp),
        Vec(x + xp, y + yp, z + zn),
        Vec(x + xp, y + yp, z + zp)
    )

    val indices = intArrayOf(
        *quad(4, 5, 7, 6), // X+
        *quad(6, 7, 3, 2), // Y+
        *quad(1, 3, 7, 5), // Z+
        *quad(2, 3, 1, 0), // X-
        *quad(0, 1, 5, 4), // Y-
        *quad(4, 6, 2, 0) // Z-
    )

    return Mesh(points, indices)
}

/**
 * Returns a primitive slope around coordinates ([x], [y], [z]) with the given or the default dimensions.
 */
fun slope(
    x: Float, y: Float, z: Float, dir: Dir,
    xn: Float = -0.5f, xp: Float = 0.5f,
    yn: Float = -0.5f, yp: Float = 0.5f,
    zn: Float = -0.5f, zp: Float = 0.5f
): Mesh {
    // TODO: Data could be optimized but I don't really care about those few bytes here.
    val points = listOf(
        Vec(x + xn, y + yn, z + zn),
        Vec(x + xn, y + yn, z + zp),
        Vec(x + xn, y + yp, z + zn),
        Vec(x + xn, y + yp, z + zp),

        Vec(x + xp, y + yn, z + zn),
        Vec(x + xp, y + yn, z + zp),
        Vec(x + xp, y + yp, z + zn),
        Vec(x + xp, y + yp, z + zp)
    )

    val indices = when (dir) {
        Dir.Up -> intArrayOf(
            *quad(0, 3, 7, 4), // Slope
            2, 3, 0, // X-
            4, 7, 6, // X+
            *quad(6, 7, 3, 2), // Y+
            *quad(4, 6, 2, 0) // Z-
        )
        Dir.Right -> intArrayOf(
            *quad(2, 7, 5, 0), // Slope
            0, 5, 4, // Y-
            6, 7, 2, // Y+
            *quad(4, 5, 7, 6), // X+
            *quad(4, 6, 2, 0) // Z-
        )
        Dir.Left -> intArrayOf(
            *quad(1, 3, 6, 4), // Slope
            0, 1, 4, // Y-
            6, 3, 2, // Y+
            *quad(2, 3, 1, 0), // X-
            *quad(4, 6, 2, 0) // Z-
        )
        Dir.Down -> intArrayOf(
            *quad(1, 2, 6, 5), // Slope
            2, 1, 0, // X-
            4, 5, 6, // X+
            *quad(0, 1, 5, 4), // Y-
            *quad(4, 6, 2, 0) // Z-
        )
    }

    return Mesh(points, indices)
}