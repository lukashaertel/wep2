package eu.metatools.ex.math

import eu.metatools.f2d.math.Pts
import eu.metatools.f2d.math.RealPt
import eu.metatools.f2d.math.toPt

/**
 * Polygon made of [RealPt].
 */
data class Poly(val points: List<RealPt>) : List<RealPt> by points, Comparable<Poly> {
    /**
     * Constructs the poly with the given values.
     */
    constructor(vararg points: RealPt) : this(points.toList())

    /**
     * Constructs the poly with the given initializer and size.
     */
    constructor(size: Int, init: (Int) -> RealPt) : this(List(size, init))

    /**
     * Polygon arranged by minimum point first.
     */
    val unified by lazy {
        val shift = points.withIndex().minBy { it.value }?.index
        if (shift == null)
            points
        else
            points.subList(shift, points.size) + points.subList(0, shift)
    }

    /**
     * Flip horizontally at x=0.
     */
    fun flipX() =
        Poly(points.map { RealPt(it.x.unaryMinus(), it.y) })

    /**
     * Flip vertically at y=0.
     */
    fun flipY() =
        Poly(points.map { RealPt(it.x, it.y.unaryMinus()) })

    /**
     * Move polygon by the given coordinate.
     */
    fun move(by: RealPt) =
        Poly(points.map { it + by })

    /**
     * Move polygon by the given coordinate.
     */
    fun move(byX: Int, byY: Int) =
        move(RealPt(byX, byY))

    /**
     * Converts the [Poly] to [Pts].
     */
    fun toPts() =
        Pts(points.size) { points[it].toPt() }

    override fun compareTo(other: Poly): Int {
        // Compare size, return already if mismatching.
        val rs = points.size.compareTo(other.points.size)
        if (rs != 0) return rs

        // Compare point-wise.
        for (i in unified.indices) {
            val rp = unified[i].compareTo(other.unified[i])
            if (rp != 0) return rp
        }

        return 0
    }

    override fun hashCode() =
        unified.hashCode()

    override fun equals(other: Any?) =
        this === other || (other as? Poly)?.unified == unified

    override fun toString() =
        points.toString()
}