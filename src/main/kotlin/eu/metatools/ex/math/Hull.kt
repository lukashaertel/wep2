package eu.metatools.ex.math

import eu.metatools.ex.math.Corner.*
import eu.metatools.f2d.math.*
import org.locationtech.jts.algorithm.distance.DistanceToPoint
import org.locationtech.jts.algorithm.distance.PointPairDistance
import org.locationtech.jts.geom.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * A list of points.
 */
typealias Poly = List<RealPt>

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
data class Collision(val support: RealPt, val distance: Real, val inside: Boolean) {
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
}

/**
 * Geometry based collision.
 */
class Hull {
    private val factory by lazy { GeometryFactory() }

    /**
     * Additives polygons.
     */
    private val plus = hashSetOf<Poly>()

    /**
     * Subtractive polygons.
     */
    private val minus = hashSetOf<Poly>()

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
            if (!plus.add(poly))
                return false

            base = null
            buffers.clear()
            return true
        }

        override fun remove(poly: Poly): Boolean {
            if (!plus.remove(poly))
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
            if (!minus.add(poly))
                return false

            base = null
            buffers.clear()
            return true
        }

        override fun remove(poly: Poly): Boolean {
            if (!minus.remove(poly))
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
        RealPt.from(x, y)

    /**
     * Converts the receiver to a polygon with the factory.
     */
    private fun Poly.toPolygon(): Polygon =
        factory.createPolygon(Array(size + 1) { i ->
            get(i.rem(size)).toCoordinate()
        })

    /**
     * Computes the SDF for the radius.
     */
    private fun computeForRadius(radius: Real): Geometry? {
        val source = base ?: run {
            if (plus.isEmpty())
                return null

            // Add all.
            val fromPlus = plus.asSequence()
                .map<Poly, Geometry> { it.toPolygon() }
                .reduce { a, b -> a.union(b) }

            // Subtract all.
            val fromMinus = minus.asSequence()
                .map<Poly, Geometry> { it.toPolygon() }
                .fold(fromPlus) { a, b -> a.difference(b) }

            // Update base and return value.
            base = fromMinus
            fromMinus
        }

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
    fun evaluate(radius: Real, pt: RealPt): Collision {
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
        RealPt.from(center.x.toDouble() + x, center.y.toDouble() + y)
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