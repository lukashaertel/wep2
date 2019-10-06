package eu.metatools.f2d.ex

import eu.metatools.f2d.math.Mat
import eu.metatools.f2d.math.Pt
import eu.metatools.f2d.math.abs
import eu.metatools.f2d.math.mapComponents
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Signed distance function for a square of a width and height of one.
 */
fun square(dim: Pt, pt: Pt): Float {
    val d = abs(pt) - dim
    val distanceOut = hypot(max(d.x, 0f), max(d.y, 0f))
    val distanceIn = min(max(d.x, d.y), 0f)
    return distanceOut + distanceIn
}

/**
 * Binds the first argument.
 */
fun square(dim: Pt = Pt.One) = { pt: Pt -> square(dim, pt) }

/**
 * Signed distance function for a circle with a diameter of one.
 */
fun circle(radius: Float, pt: Pt) =
    pt.len - radius

/**
 * Binds the first argument.
 */
fun circle(radius: Float = 0.5f) = { pt: Pt -> circle(radius, pt) }

/**
 * Multiplies the signed distance function with a matrix.
 */
inline operator fun Mat.times(crossinline f: (Pt) -> Float) = { pt: Pt ->
    f(this * pt)
}

/**
 * Returns the union function on the given functions.
 */
fun union(vararg fs: (Pt) -> Float) = { pt: Pt ->
    var min = Float.POSITIVE_INFINITY
    for (f in fs)
        min = minOf(min, f(pt))
    min
}

/**
 * Returns the union function on the given functions.
 */
fun union(fs: Sequence<(Pt) -> Float>) = { pt: Pt ->
    var min = Float.POSITIVE_INFINITY
    for (f in fs)
        min = minOf(min, f(pt))
    min
}

/**
 * Returns the union function on the given functions.
 */
fun union(fs: Iterable<(Pt) -> Float>) = { pt: Pt ->
    var min = Float.POSITIVE_INFINITY
    for (f in fs)
        min = minOf(min, f(pt))
    min
}

/**
 * Epsilon value for derivative calculation.
 */
private val e = 1f / 2.shl(16)

/**
 * Epsilon as X component.
 */
private val ex = Pt(x = e)

/**
 * Epsilon as Y component.
 */
private val ey = Pt(y = e)

/**
 * Computes the derivative of [f] at [pt].
 */
inline fun derivative(f: (Pt) -> Float, pt: Pt): Pt {
    val fv = f(pt)
    @Suppress("non_public_call_from_public_inline")
    return Pt((f(pt + ex) - fv) / e, (f(pt + ey) - fv) / e)
}

/**
 * Finds the root of [f], starting at [pt].
 */
inline fun root(f: (Pt) -> Float, ipt: Pt, maxIter: Int = 8, o: Float = 0.8125f): Pt {
    // Compute reused value.
    val io = 1f - o

    // Initialize iteration point.
    var pt = ipt

    // Repeat a maximum number of times.
    repeat(maxIter) {
        // Get value of point.
        val fv = f(pt)

        // If in epsilon range, return immediately.
        @Suppress("non_public_call_from_public_inline")
        if (abs(fv) < e)
            return pt

        // Compute f over derivatives.
        val dis = Pt(fv, fv) / derivative(f, pt).mapComponents {
            if (it == 0f) Float.POSITIVE_INFINITY else it
        }


        // Displace point, smooth to prevent overshoot.
        @Suppress("non_public_call_from_public_inline")
        pt = pt * io + (pt - if (dis.isEmpty) pt * e else dis) * o
    }

    // Return last best point.
    return pt
}


fun main() {
    val parts = (0..10).map { { pt: Pt -> square(Pt(1.0f, 0.5f), pt + Pt.X * it.toFloat() * 0.5f) } }
    val df = union(parts)
    val p = root(df, Pt(-0.25f, 0.2f))
    println(p)

}