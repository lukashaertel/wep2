package eu.metatools.ex.geom

import eu.metatools.ex.data.Dir
import eu.metatools.f2d.data.Vec
import eu.metatools.f2d.data.Vecs

/**
 * A pair of points and triangle indices.
 * @property points The points of the mesh.
 * @param indices The indices of the mesh.
 */
data class Mesh(val points: List<Vec>, val indices: IntArray) : Sequence<Vecs> {
    val center by lazy {
        // Initialize aggregator.
        var x = 0.0
        var y = 0.0
        var z = 0.0
        var count = 0

        // Iterate points with indices.
        for ((i, v) in points.withIndex()) {
            // If index not used, skip this point.
            if (i !in indices)
                continue

            // Add to aggregator.
            x += v.x
            y += v.y
            z += v.z
            count++
        }

        // Return the center.
        Vec((x / count).toFloat(), (y / count).toFloat(), (z / count).toFloat())
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

    override fun iterator() = object : Iterator<Vecs> {
        private val current = Vecs(3)

        var index = 0
        override fun hasNext() =
            index + 2 < indices.size

        override fun next(): Vecs {
            val p1 = points[indices[index]]
            val p2 = points[indices[index + 1]]
            val p3 = points[indices[index + 2]]
            index += 3
            current.values[0] = p1.x
            current.values[1] = p1.y
            current.values[2] = p1.z
            current.values[3] = p2.x
            current.values[4] = p2.y
            current.values[5] = p2.z
            current.values[6] = p3.x
            current.values[7] = p3.y
            current.values[8] = p3.z
            return current
        }
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

fun Mesh.rotate90(x: Float, y: Float) =
    Mesh(points.map { Vec((it.y - y) + x, -(it.x - x) + y, it.z) }, indices)

fun Mesh.rotate180(x: Float, y: Float) =
    Mesh(points.map { Vec(-(it.x - x) + x, -(it.y - y) + y, it.z) }, indices)

fun Mesh.rotate270(x: Float, y: Float) =
    Mesh(points.map { Vec(-(it.y - y) + x, (it.x - x) + y, it.z) }, indices)

fun Mesh.rotate(x: Float, y: Float, dir: Dir) = when (dir) {
    Dir.Right -> this
    Dir.Down -> rotate90(x, y)
    Dir.Left -> rotate180(x, y)
    Dir.Up -> rotate270(x, y)
}

/**
 * Returns a primitive slope around coordinates ([x], [y], [z]) with the given or the default dimensions.
 */
fun slope(
    x: Float, y: Float, z: Float,
    xn: Float = -0.5f, xp: Float = 0.5f,
    yn: Float = -0.5f, yp: Float = 0.5f,
    zn: Float = -0.5f, zp: Float = 0.5f
): Mesh {
    val points = listOf(
        Vec(x + xn, y + yn, z + zn),
        Vec(x + xn, y + yp, z + zn),

        Vec(x + xp, y + yn, z + zn),
        Vec(x + xp, y + yn, z + zp),
        Vec(x + xp, y + yp, z + zn),
        Vec(x + xp, y + yp, z + zp)
    )

    val indices = intArrayOf(
        *quad(1, 5, 3, 0),
        *quad(2, 3, 5, 4),
        *quad(0, 2, 4, 1),
        0, 3, 2,
        1, 4, 5
    )

    return Mesh(points, indices)
}

/**
 * Returns the box around coordinates ([x], [y], [z]) with the given or the default dimensions.
 */
fun slopeStump(
    x: Float, y: Float, z: Float,
    xn: Float = -0.5f, xp: Float = 0.5f,
    yn: Float = -0.5f, yp: Float = 0.5f,
    zn: Float = -0.5f, zpFrom: Float = 0.5f, zpTo: Float = 0.5f
): Mesh {
    val points = listOf(
        Vec(x + xn, y + yn, z + zn),
        Vec(x + xn, y + yn, z + zpFrom),
        Vec(x + xn, y + yp, z + zn),
        Vec(x + xn, y + yp, z + zpFrom),

        Vec(x + xp, y + yn, z + zn),
        Vec(x + xp, y + yn, z + zpTo),
        Vec(x + xp, y + yp, z + zn),
        Vec(x + xp, y + yp, z + zpTo)
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
