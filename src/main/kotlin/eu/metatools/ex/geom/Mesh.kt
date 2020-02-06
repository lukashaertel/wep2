package eu.metatools.ex.geom

import eu.metatools.ex.data.Dir
import eu.metatools.f2d.data.Vec
import eu.metatools.f2d.data.Vecs
import eu.metatools.f2d.data.mapVecs

/**
 * A pair of points and triangle indices.
 * @property points The points of the mesh.
 * @param indices The indices of the mesh.
 */
data class Mesh(val points: Vecs, val indices: IntArray) {
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

    private fun iterator() = object : Iterator<Vecs> {
        var index = 0
        override fun hasNext() =
            index + 2 < indices.size

        override fun next(): Vecs {
            index += 3
            return Vecs(
                points[indices[index - 3]],
                points[indices[index - 2]],
                points[indices[index - 1]]
            )
        }
    }

    /**
     * Interprets the mesh as an iterable of triangles.
     */
    fun asIterable() = Iterable { iterator() }

    /**
     * Interprets the mesh as a sequence of triangles.
     */
    fun asSequence() = Sequence { iterator() }
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
    val points = Vecs(
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
    Mesh(points.mapVecs { Vec((it.y - y) + x, -(it.x - x) + y, it.z) }, indices)

fun Mesh.rotate180(x: Float, y: Float) =
    Mesh(points.mapVecs { Vec(-(it.x - x) + x, -(it.y - y) + y, it.z) }, indices)

fun Mesh.rotate270(x: Float, y: Float) =
    Mesh(points.mapVecs { Vec(-(it.y - y) + x, (it.x - x) + y, it.z) }, indices)

fun Mesh.rotate(x: Float, y: Float, dir: Dir) = when (dir) {
    Dir.Right -> this
    Dir.Down -> rotate90(x, y)
    Dir.Left -> rotate180(x, y)
    Dir.Up -> rotate270(x, y)
}

/**
 * Filters the triangles, returns a new mesh.
 */
fun Mesh.filter(block: (Vecs) -> Boolean): Mesh {
    val points = arrayListOf<Vec>()
    val indices = arrayListOf<Int>()
    for (vecs in asIterable())
        if (block(vecs)) {
            val i1 = points.indexOf(vecs[0]).takeIf { it >= 0 } ?: points.size.also { points.add(vecs[0]) }
            val i2 = points.indexOf(vecs[1]).takeIf { it >= 0 } ?: points.size.also { points.add(vecs[1]) }
            val i3 = points.indexOf(vecs[2]).takeIf { it >= 0 } ?: points.size.also { points.add(vecs[2]) }
            indices.add(i1)
            indices.add(i2)
            indices.add(i3)
        }

    return Mesh(Vecs(points), indices.toIntArray())
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
    val points = Vecs(
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
    val points = Vecs(
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
