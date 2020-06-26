package eu.metatools.ex.geom

import eu.metatools.fio.data.Vec
import eu.metatools.fio.data.Vecs


/**
 * Returns a predicate that matches the normal of the given triangle with the given [normal]. Requires [limit] to be
 * less than or equal to the dot product of triangle and given normal.
 */
fun matchNormal(normal: Vec, limit: Float = 0f) = { (v1, v2, v3): Vecs ->
    // Limit must be less than the dot product of the normal and the triangle normal.
    limit <= (v3 - v1 cross v2 - v1).nor dot normal
}

/**
 * Matches dot product equal to one.
 */
val isXP by lazy { matchNormal(Vec.X, 1f) }

/**
 * Matches dot product equal to one.
 */
val isXN by lazy { matchNormal(-Vec.X, 1f) }

/**
 * Matches dot product equal to one.
 */
val isYP by lazy { matchNormal(Vec.Y, 1f) }

/**
 * Matches dot product equal to one.
 */
val isYN by lazy { matchNormal(-Vec.Y, 1f) }

/**
 * Matches dot product equal to one.
 */
val isZP by lazy { matchNormal(Vec.Z, 1f) }

/**
 * Matches dot product equal to one.
 */
val isZN by lazy { matchNormal(-Vec.Z, 1f) }