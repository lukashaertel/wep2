package eu.metatools.ex.math

import eu.metatools.f2d.math.*
import kotlin.math.abs

/**
 * Signed distance function for a square of a width and height of one.
 */
fun square(dim: RealPt, pt: RealPt): Real {
    val d = abs(pt) - dim
    val distanceOut = hypot(max(d.x, Real.Zero), max(d.y, Real.Zero))
    val distanceIn = min(max(d.x, d.y), Real.Zero)
    return distanceOut + distanceIn
}

/**
 * Applies a located square.
 */
fun squareFromTo(start: RealPt, end: RealPt, pt: RealPt): Real {
    val dim = (end - start) / 2
    val center = (start + end) / 2
    return square(dim, pt - center)
}

/**
 * Binds the first argument.
 */
fun square(dim: RealPt = RealPt.One) = { pt: RealPt -> square(dim, pt) }

/**
 * Signed distance function for a circle with a diameter of one.
 */
fun circle(radius: Real, pt: RealPt) =
    pt.len - radius

/**
 * Binds the first argument.
 */
fun circle(radius: Real = 0.5f.toReal()) = { pt: RealPt -> circle(radius, pt) }

/**
 * Returns the union function on the given functions.
 */
fun union(vararg fs: (RealPt) -> Real) = { pt: RealPt ->
    var min = Real.MAX_VALUE
    for (f in fs)
        min = min(min, f(pt))
    min
}

/**
 * Returns the union function on the given functions.
 */
fun union(fs: Sequence<(RealPt) -> Real>) = { pt: RealPt ->
    var min = Real.MAX_VALUE
    for (f in fs)
        min = min(min, f(pt))
    min
}

/**
 * Returns the union function on the given functions.
 */
fun union(fs: Iterable<(RealPt) -> Real>) = { pt: RealPt ->
    var min = Real.MAX_VALUE
    for (f in fs)
        min = min(min, f(pt))
    min
}

/**
 * Epsilon value for derivative calculation.
 */
private val e = Real(1)

/**
 * Epsilon as X component.
 */
private val ex = RealPt(e, Real.Zero)

/**
 * Epsilon as Y component.
 */
private val ey = RealPt(Real.Zero, e)

/**
 * Computes the derivative of [f] at [pt].
 */
inline fun derivative(f: (RealPt) -> Real, pt: RealPt): RealPt {
    val fv = f(pt)
    @Suppress("non_public_call_from_public_inline")
    return RealPt((f(pt + ex) - fv) / e, (f(pt + ey) - fv) / e)
}

/**
 * Finds the root of [f], starting at [pt].
 */
inline fun root(f: (RealPt) -> Real, ipt: RealPt, maxIter: Int = 8, o: Real = 0.8125f.toReal()): RealPt {
    // Compute reused value.
    val io = Real.One - o

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
        val dis = RealPt(fv, fv) / derivative(f, pt).mapComponents {
            if (it == Real.Zero) Real.MAX_VALUE else it
        }

        // Displace point, smooth to prevent overshoot.
        @Suppress("non_public_call_from_public_inline")
        pt = pt * io + (pt - if (dis.isEmpty) pt * e else dis) * o
    }

    // Return last best point.
    return pt
}