package eu.metatools.ex.ents

import eu.metatools.ex.input.toInt
import org.locationtech.jts.algorithm.distance.DistanceToPoint
import org.locationtech.jts.algorithm.distance.PointPairDistance
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel

class Hull {
    companion object {
        private const val precision = 1000.0
        private const val epsilon = 2.0 / precision
    }

    /**
     * Geometry factory for computations.
     */
    private val factory = GeometryFactory(PrecisionModel(1000.0))

    /**
     * Base level definitons.
     */
    private val levels = mutableMapOf<Int, Geometry>()

    /**
     * Buffers with radii applied.
     */
    private val buffers = mutableMapOf<Int, MutableMap<Double, Geometry?>>()

    /**
     * Computes a square around the given center.
     */
    private fun square(x: Number, y: Number) =
        factory.createPolygon(
            arrayOf(
                Coordinate(x.toDouble() - 0.5, y.toDouble() - 0.5),
                Coordinate(x.toDouble() - 0.5, y.toDouble() + 0.5),
                Coordinate(x.toDouble() + 0.5, y.toDouble() + 0.5),
                Coordinate(x.toDouble() + 0.5, y.toDouble() - 0.5),
                Coordinate(x.toDouble() - 0.5, y.toDouble() - 0.5)
            )
        )

    fun add(level: Int, x: Number, y: Number) {
        val square = square(x, y)
        levels.compute(level) { _, p -> p?.union(square) ?: square }
        buffers.remove(level)
    }

    fun remove(level: Int, x: Number, y: Number) {
        val square = square(x, y)
        levels.computeIfPresent(level) { _, p -> p.difference(square) }
        buffers.remove(level)
    }

    private fun buffer(level: Int, distance: Double): Geometry? {
        return buffers.getOrPut(level, ::mutableMapOf).getOrPut(distance) {
            levels[level]?.buffer(distance)
        }
    }

    /**
     * Returns for the given coordinate, [level] and [radius] the point that needs to be reset to to maintain
     * [inside]ness of the coordinate. E.g., if [inside] is given as true, the circles not contained in the geometry are
     * clipped so they are. If given false, circles inside are moved to the border.
     */
    fun bindIn(level: Int, radius: Number, x: Number, y: Number): Pair<Number, Number>? {
        // Get distance from radius and inversion.
        val distance = -radius.toDouble()

        // Get buffer, if inverted mode use negative radius.
        val geometry = buffer(level, distance)
            ?: return null

        // Convert the coordinate.
        val coord = Coordinate(x.toDouble(), y.toDouble())

        // If inside geometry, no binding required.
        if (geometry.contains(factory.createPoint(coord)))
            return null

        // Compute distance if not contained, also returns the support vector.
        val support = PointPairDistance().also {
            DistanceToPoint.computeDistance(geometry, coord, it)
        }

        // Return support vector.
        return support.getCoordinate(0).let {
            it.x to it.y
        }
    }

    fun bindOut(level: Int, radius: Number, x: Number, y: Number): Pair<Number, Number>? {
        // Get distance from radius and inversion.
        val distance = radius.toDouble()

        // Get buffer, if inverted mode use negative radius.
        val geometry = buffer(level, distance)
            ?: return null

        // Convert the coordinate.
        val coord = Coordinate(x.toDouble(), y.toDouble())

        // If inside geometry, no binding required.
        if (!geometry.contains(factory.createPoint(coord)))
            return null

        // Compute distance if not contained, also returns the support vector.
        val support = PointPairDistance().also {
            DistanceToPoint.computeDistance(geometry, coord, it)
        }

        // Return support vector.
        return support.getCoordinate(0).let {
            it.x to it.y
        }
    }

    fun integrate(level: Int, radius: Number, x: Number, y: Number, inside: Boolean = false): Pair<Number, Number> {
        // Get distance from radius and inversion.
        val distance = if (inside) -radius.toDouble() else radius.toDouble()

        // Get geometry or return null.
        val geometry = buffer(level, distance)
            ?: return 0 to 0

        // Convert x and y to doubles.
        val xd = x.toDouble()
        val yd = y.toDouble()

        // Check for containment along epsilon.
        val l = geometry.contains(factory.createPoint(Coordinate(xd - epsilon, yd))).toInt()
        val t = geometry.contains(factory.createPoint(Coordinate(xd, yd + epsilon))).toInt()
        val r = geometry.contains(factory.createPoint(Coordinate(xd + epsilon, yd))).toInt()
        val b = geometry.contains(factory.createPoint(Coordinate(xd, yd - epsilon))).toInt()

        // Return difference.
        return (l - r) to (b - t)
    }
}