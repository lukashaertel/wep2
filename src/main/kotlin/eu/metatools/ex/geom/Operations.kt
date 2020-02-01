package eu.metatools.ex.geom

import eu.metatools.ex.math.td
import eu.metatools.f2d.data.Vec
import eu.metatools.f2d.data.Vecs
import kotlin.math.abs

/**
 * Filters the triangles of the mesh with a [normal] and a [limit] that the dot product of the normalized triangle
 * normal may not be less than.
 */
fun Sequence<Vecs>.filterNormal(normal: Vec, limit: Float = 0f) =
    filter { (v1, v2, v3) ->
        // Limit must be less than the dot product of the normal and the triangle normal.
        limit <= (v3 - v1 cross v2 - v1).nor dot normal
    }

/**
 * Returns true if the point is inside the mesh. The mesh must be convex.
 */
fun Sequence<Vecs>.inside(pos: Vec, radius: Float): Boolean {
    // If above any triangle, return false.
    forEach { (v1, v2, v3) ->
        val n = (v3 - v1 cross v2 - v1).nor
        val d = (pos - v1) dot n

        if (d >= radius)
            return false
    }

    // Return true, under all triangles.
    return true
}

/**
 * Returns true if the point is inside the mesh. The mesh must be convex.
 */
fun Sequence<Vecs>.depth(pos: Vec, radius: Float): Pair<Vec, Float>? {
    var closest = Float.MAX_VALUE
    var normal = Vec.Zero

    // If above any triangle, return false.
    for ((v1, v2, v3) in this) {
        // Get components
        val v1toPos = pos - v1
        val v2toPos = pos - v2
        val v3toPos = pos - v3
        val v1to2 = v2 - v1
        val v2to3 = v3 - v2
        val v3to1 = v1 - v3

        // Compute triangle normal.
        val n = v1to2 cross v3to1
        val d = n dot v1toPos

        if (d >= radius)
            return null

        val s1 = v1to2 cross n
        val s2 = v2to3 cross n
        val s3 = v3to1 cross n

        // Skip this triangle if not under it.
        if (s1 dot v1toPos < 0f || s2 dot v2toPos < 0f || s3 dot v3toPos < 0f)
            continue

        // Determine penetration, replace if better.
        val penetration = radius - d
        if (penetration < closest) {
            closest = penetration
            normal = n
        }
    }

    // Return true, under all triangles.
    return if (closest < Float.MAX_VALUE) normal to closest else null
}

/**
 * Returns the triangle with the smallest absolute distance to the point [pos]. Limits the selection of triangles by a
 * given [dir] and a lower limit of the dot product of normal and [dir].
 */
@Deprecated("Deprecated functionality")
fun Mesh.closest(pos: Vec, radius: Float, dir: Vec = Vec.Zero, limit: Float = Float.MIN_VALUE): Pair<Vecs, Float> {
    // Tracked maximal values.
    var best = Float.MAX_VALUE
    val result = Vecs(3)

    // Iterate all triangles, underwrite if closer.
    forEach { v1, v2, v3 ->
        val d = td(pos, v1, v2, v3) - radius
//        val n = (v3 - v1 cross v2 - v1).nor
//        if (true||n dot dir > limit) {
//            val d = (pos - v1) dot n - radius

        if (abs(d) < best) {
            best = d
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
//        }
    }

    // Return the minimized vectors with the distance.
    return result to best
}

/**
 * Returns the triangle with the smallest absolute distance to the point [pos].
 */
@Deprecated(
    "Deprected overload",
    ReplaceWith("closest(pos, 0f, Vec.Zero, Float.MIN_VALUE)", "eu.metatools.f2d.data.Vec"),
    DeprecationLevel.ERROR
)
fun Mesh.closest(pos: Vec): Pair<Vecs, Float> {
    // Tracked maximal values.
    var maximal = Float.MIN_VALUE
    val result = Vecs(3)

    // Iterate all triangles, underwrite if closer.
    forEach { v1, v2, v3 ->
        val n = (v3 - v1 cross v2 - v1).nor
        val d = (pos - v1) dot n
        // TODO: Maybe perform insideness check with triangle projection?

        if (d > maximal) {
            maximal = d
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
    return result to maximal
}