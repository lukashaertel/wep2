package eu.metatools.ex.math

import eu.metatools.f2d.math.Pt
import eu.metatools.f2d.math.Real
import eu.metatools.f2d.math.RealPt
import eu.metatools.f2d.math.toReal

/**
 * A segment of the [SDFComposer].
 * @property x The horizontal start of the segment.
 * @property y The vertical start of the segment.
 * @property length The length of the segment, including the start.
 * @property vertical True if the segment is vertical.
 */
data class SDFSegment(val x: Int, val y: Int, val length: Int, val vertical: Boolean) {
    /**
     * True if the point is just before the segment.
     */
    fun extendsStart(x: Int, y: Int) =
        if (vertical)
            this.x == x && y + 1 == this.y
        else
            this.y == y && x + 1 == this.x

    /**
     * True if the point is just after the segment.
     */
    fun extendsEnd(x: Int, y: Int) =
        if (vertical)
            this.x == x && this.y + length == y
        else
            this.y == y && this.x + length == x

    /**
     * True if the point is the start of the segment.
     */
    fun starts(x: Int, y: Int) =
        this.x == x && this.y == y

    /**
     * True if the point is the end of the segment.
     */
    fun ends(x: Int, y: Int) =
        if (vertical)
            this.x == x && this.y + length - 1 == y
        else
            this.y == y && this.x + length - 1 == x

    /**
     * True if the point is on the segment but neither start nor end.
     */
    fun inside(x: Int, y: Int) =
        if (vertical)
            this.x == x && this.y < y && y < this.y + length - 1
        else
            this.y == y && this.x < x && x < this.x + length - 1

    /**
     * True if the point is on the segment.
     */
    fun contains(x: Int, y: Int) =
        if (vertical)
            this.x == x && this.y <= y && y <= this.y + length - 1
        else
            this.y == y && this.x <= x && x <= this.x + length - 1

    /**
     * True if the other segment covers the area of this segment.
     */
    fun coveredBy(segment: SDFSegment) =
        if (length == 1) {
            segment.contains(x, y)
        } else {
            if (vertical)
                segment.vertical && x == segment.x && segment.y <= y && y + length <= segment.y + segment.length
            else
                segment.horizontal && y == segment.y && segment.x <= x && x + length <= segment.x + segment.length
        }

    /**
     * True if the segment has a length of zero.
     */
    val isEmpty get() = length == 0

    /**
     * True if the segment has a positive length.
     */
    val isNotEmpty get() = length > 0

    /**
     * True if the segment is not vertical.
     */
    val horizontal get() = !vertical

    /**
     * Removes a bit of the end.
     */
    fun shrinkEnd() = SDFSegment(x, y, length - 1, vertical)

    /**
     * Adds a bit to the end.
     */
    fun extendEnd() = SDFSegment(x, y, length + 1, vertical)

    /**
     * Moves the start to the point.
     */
    fun cutStart(x: Int, y: Int) =
        if (vertical)
            SDFSegment(this.x, y, this.y + length - y, vertical)
        else
            SDFSegment(x, this.y, this.x + length - x, vertical)

    /**
     * Moves the end to the point.
     */
    fun cutEnd(x: Int, y: Int) =
        if (vertical)
            SDFSegment(this.x, this.y, y - this.y, vertical)
        else
            SDFSegment(this.x, this.y, x - this.x, vertical)

    /**
     * Removes a bit of the start.
     */
    fun shrinkStart() =
        if (vertical)
            SDFSegment(x, y + 1, length - 1, vertical)
        else
            SDFSegment(x + 1, y, length - 1, vertical)

    /**
     * Adds a bit to the start.
     */
    fun extendStart() =
        if (vertical)
            SDFSegment(x, y - 1, length + 1, vertical)
        else
            SDFSegment(x - 1, y, length + 1, vertical)

    /**
     * Returns the SDF for this segment.
     */
    fun sdf(radiusX: Real, radiusY: Real) =
        if (vertical)
            { pt: RealPt ->
                squareFromTo(
                    RealPt(x.toReal() - radiusX, y.toReal() - radiusY),
                    RealPt(x.toReal() + radiusX, (y + length - 1).toReal() + radiusY),
                    pt
                )
            }
        else
            { pt: RealPt ->
                squareFromTo(
                    RealPt(x.toReal() - radiusX, y.toReal() - radiusY),
                    RealPt((x + length - 1).toReal() + radiusX, y.toReal() + radiusY),
                    pt
                )
            }

}

/**
 * Composes block based SDF segments of the given [radiusX] x [radiusY].
 */
class SDFComposer(val radiusX: Real = 0.5f.toReal(), val radiusY: Real = 0.5f.toReal()) {
    /**
     * Returns the SDF for a given object radius.
     */
    fun sdf(radius: Real): (RealPt) -> Real {
        // Get valid segments, i.e., not covered by another segment.
        val validSegments = segments.filterIndexed { i, segment ->
            segments.subList(i + 1, segments.size).none {
                segment.coveredBy(it)
            }
        }

        // Get the SDFs.
        val segmentSDFs = validSegments.map {
            it.sdf(radiusX + radius, radiusY + radius)
        }

        // Return the union, also cache result.
        return union(segmentSDFs)
    }

    /**
     * The list of all segments.
     */
    private val segments = mutableListOf<SDFSegment>()

    /**
     * Adds a block, computes the segments.
     */
    fun add(x: Int, y: Int) {
        for (vertical in sequenceOf(false, true)) {
            val start = segments.find { it.vertical == vertical && it.extendsStart(x, y) }
            val end = segments.find { it.vertical == vertical && it.extendsEnd(x, y) }

            if (start != null && end != null) {
                segments.remove(start)
                segments.remove(end)
                segments.add(
                    SDFSegment(
                        end.x,
                        end.y,
                        end.length + 1 + start.length,
                        vertical
                    )
                )
            } else if (start != null) {
                segments.remove(start)
                segments.add(start.extendStart())
            } else if (end != null) {
                segments.remove(end)
                segments.add(end.extendEnd())
            } else {
                segments.add(SDFSegment(x, y, 1, vertical))
            }
        }
    }

    /**
     * Removes a block, computes the segments.
     */
    fun remove(x: Int, y: Int) {
        for (vertical in sequenceOf(false, true)) {
            val start = segments.find { it.vertical == vertical && it.starts(x, y) }
            val split = segments.find { it.vertical == vertical && it.inside(x, y) }
            val end = segments.find { it.vertical == vertical && it.ends(x, y) }

            if (start != null) {
                segments.remove(start)
                val replacement = start.shrinkStart()
                if (replacement.isNotEmpty)
                    segments.add(replacement)
            }
            if (split != null) {
                segments.remove(split)
                val replacementStart = split.cutStart(x, y)
                val replacementEnd = split.cutEnd(x, y)
                if (replacementStart.isNotEmpty)
                    segments.add(replacementStart)
                if (replacementEnd.isNotEmpty)
                    segments.add(replacementEnd)
            }
            if (end != null) {
                segments.remove(end)
                val replacement = end.shrinkEnd()
                if (replacement.isNotEmpty)
                    segments.add(replacement)
            }
        }
    }
}