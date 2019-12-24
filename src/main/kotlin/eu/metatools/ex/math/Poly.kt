package eu.metatools.ex.math

import eu.metatools.f2d.math.Pts
import eu.metatools.f2d.math.RealPt
import eu.metatools.f2d.math.toPt

/**
 * A list of points.
 */
typealias Poly = List<RealPt>

fun Poly.flipX() =
    map { RealPt(it.x.unaryMinus(), it.y) }

fun Poly.flipY() =
    map { RealPt(it.x, it.y.unaryMinus()) }

fun Poly.move(by: RealPt) =
    map { it + by }

fun Poly.move(byX: Int, byY: Int) =
    move(RealPt(byX, byY))

/**
 * Converts the [Poly] to [Pts].
 */
fun Poly.toPts() =
    Pts(size) { get(it).toPt() }
