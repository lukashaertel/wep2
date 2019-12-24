package eu.metatools.ex.math

import eu.metatools.ex.math.Corner.*
import eu.metatools.f2d.math.*
import org.locationtech.jts.algorithm.distance.DistanceToPoint
import org.locationtech.jts.algorithm.distance.PointPairDistance
import org.locationtech.jts.geom.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * A polygon group that can be added to or removed from.
 */
interface PolyGroup {
    /**
     * Adds a polygon.
     */
    fun add(poly: Poly): Boolean

    /**
     * Removes a polygon.
     */
    fun remove(poly: Poly): Boolean
}

/**
 * A collision with point, distance and flag if inside.
 */
data class Collision(val support: RealPt, val distance: Real, val inside: Boolean) : Comparable<Collision> {
    companion object {
        /**
         * No collision.
         */
        val NONE = Collision(RealPt.ZERO, Real.MAX_VALUE, false)
    }

    /**
     * Returns a negative distance if inside the geometry.
     */
    val signedDistance
        get() = if (inside) -distance else distance

    override fun compareTo(other: Collision): Int {
        val rd = signedDistance.compareTo(other.signedDistance)
        if (rd != 0) return rd
        val rs = support.compareTo(other.support)
        return rs
    }
}

/**
 * Geometry based collision.
 */
class Regions {
    private val factory by lazy { GeometryFactory() }

    /**
     * Additives polygons.
     */
    private val plusSet = hashSetOf<Poly>()

    /**
     * Subtractive polygons.
     */
    private val minusSet = hashSetOf<Poly>()

    /**
     * Gets a copy of the additive polygons.
     */
    val plus get() = plusSet.toSet()

    /**
     * Gets a copy of the subtractive polygons.
     */
    val minus get() = minusSet.toSet()

    /**
     * The base geometry or null if not computed.
     */
    private var base: Geometry? = null

    /**
     * The buffer polygons per radius.
     */
    private val buffers = hashMapOf<Real, Geometry?>()

    /**
     * Set of additive polygons.
     */
    val union = object : PolyGroup {
        override fun add(poly: Poly): Boolean {
            if (!plusSet.add(poly))
                return false

            base = null
            buffers.clear()
            return true
        }

        override fun remove(poly: Poly): Boolean {
            if (!plusSet.remove(poly))
                return false

            base = null
            buffers.clear()
            return true
        }
    }

    /**
     * Set of subtractive polygons.
     */
    val subtract = object : PolyGroup {
        override fun add(poly: Poly): Boolean {
            if (!minusSet.add(poly))
                return false

            base = null
            buffers.clear()
            return true
        }

        override fun remove(poly: Poly): Boolean {
            if (!minusSet.remove(poly))
                return false

            base = null
            buffers.clear()
            return true
        }
    }

    /**
     * Converts the receiver to a coordinate.
     */
    private fun RealPt.toCoordinate(): Coordinate =
        Coordinate(x.toDouble(), y.toDouble())

    /**
     * Converts the receiver to a coordinate.
     */
    private fun Coordinate.toReal(): RealPt =
        RealPt(x, y)

    /**
     * Converts the receiver to a polygon with the factory.
     */
    private fun Poly.toGeometry(): Geometry? =
        factory.createPolygon(Array(size + 1) {
            get(it.rem(size)).toCoordinate()
        })


    /**
     * Computes the SDF for the radius.
     */
    private fun computeForRadius(radius: Real): Geometry? {
        val source = base ?: run {
            if (plusSet.isEmpty())
                return null

            // Add all.
            val fromPlus = plusSet.asSequence()
                .mapNotNull { it.toGeometry() }
                .reduce { a, b -> a.union(b) }

            // Subtract all.
            val fromMinus = minusSet.asSequence()
                .mapNotNull { it.toGeometry() }
                .fold(fromPlus) { a, b -> a.difference(b) }

            // Update base and return value.
            base = fromMinus
            fromMinus
        }

        // No radius, return the value immediately.
        if (radius == Real.ZERO)
            return source

        // Get the buffered geometry.
        return source.buffer(radius.toDouble())
    }

    /**
     * Gets the buffer geometry for a radius.
     */
    private fun forRadius(radius: Real) =
        buffers.getOrPut(radius) { computeForRadius(radius) }

    /**
     * Takes an object radius [radius] and a center [pt]. Returns the distance and the supporting point.
     */
    fun collision(radius: Real, pt: RealPt): Collision {
        // Get geometry or return default.
        val geometry = forRadius(radius)
            ?: return Collision.NONE

        // Get coordinate from point.
        val coordinate = pt.toCoordinate()

        // Compute distance.
        val output = PointPairDistance()
        DistanceToPoint.computeDistance(geometry, coordinate, output)

        // Compute if inside.
        val inside = geometry.contains(factory.createPoint(coordinate))

        // Return distance and supporting point.
        return Collision(output.getCoordinate(0).toReal(), output.distance.toReal(), inside)
    }

    /**
     *  Takes an object radius [radius] and a center [pt]. Returns true if contained.
     */
    fun contains(radius: Real, pt: RealPt): Boolean {
        // Get geometry or return default.
        val geometry = forRadius(radius)
            ?: return false

        // Get coordinate from point.
        val coordinate = pt.toCoordinate()

        // Return if point is contained.
        return geometry.contains(factory.createPoint(coordinate))
    }
}

/**
 * Computes the rectangle.
 */
fun polyRect(from: RealPt, to: RealPt): Poly {
    val minX = min(from.x, to.x)
    val maxX = max(from.x, to.x)
    val minY = min(from.y, to.y)
    val maxY = max(from.y, to.y)
    return listOf(RealPt(minX, minY), RealPt(maxX, minY), RealPt(maxX, maxY), RealPt(minX, maxY))
}

/**
 * Computes the circle.
 */
fun polyCircle(center: RealPt, radius: Real, segments: Int = 16): Poly {
    val ss = Math.PI * 2 / segments
    return List(segments - 1) { i ->
        val x = cos(ss * i) * radius.toDouble()
        val y = sin(ss * i) * radius.toDouble()
        RealPt(center.x.toDouble() + x, center.y.toDouble() + y)
    }
}

/**
 * The corner of a wedge.
 */
enum class Corner {
    /**
     * Top-left corner is the 90 degree angle.
     */
    TopLeft,
    /**
     * Top-right corner is the 90 degree angle.
     */
    TopRight,
    /**
     * Bottom-right corner is the 90 degree angle.
     */
    BottomRight,
    /**
     * Bottom-left corner is the 90 degree angle.
     */
    BottomLeft
}

/**
 * Computes the wedge.
 */
fun polyWedge(from: RealPt, to: RealPt, corner: Corner): Poly {
    val minX = min(from.x, to.x)
    val maxX = max(from.x, to.x)
    val minY = min(from.y, to.y)
    val maxY = max(from.y, to.y)

    return when (corner) {
        BottomLeft -> listOf(RealPt(minX, minY), RealPt(maxX, minY), RealPt(minX, maxY))
        BottomRight -> listOf(RealPt(minX, minY), RealPt(maxX, minY), RealPt(maxX, maxY))
        TopRight -> listOf(RealPt(maxX, minY), RealPt(maxX, maxY), RealPt(minX, maxY))
        TopLeft -> listOf(RealPt(minX, minY), RealPt(maxX, maxY), RealPt(minX, maxY))
    }
}

/**
 * Adds a rectangle to the polygon group.
 */
fun PolyGroup.addRect(from: RealPt, to: RealPt) =
    add(polyRect(from, to))

/**
 * Removes a rectangle from the polygon group.
 */
fun PolyGroup.removeRect(from: RealPt, to: RealPt) =
    remove(polyRect(from, to))

/**
 * Adds a circle to the polygon group.
 */
fun PolyGroup.addCircle(center: RealPt, radius: Real, segments: Int = 16) =
    add(polyCircle(center, radius, segments))

/**
 * Removes a circle from the polygon group.
 */
fun PolyGroup.removeCircle(center: RealPt, radius: Real, segments: Int = 16) =
    remove(polyCircle(center, radius, segments))


/**
 * Adds a wedge to the polygon group.
 */
fun PolyGroup.addWedge(from: RealPt, to: RealPt, corner: Corner) =
    add(polyWedge(from, to, corner))

/**
 * Removes a wedge from the polygon group.
 */
fun PolyGroup.removeWedge(from: RealPt, to: RealPt, corner: Corner) =
    remove(polyWedge(from, to, corner))