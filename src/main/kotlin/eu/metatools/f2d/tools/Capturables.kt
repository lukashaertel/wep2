package eu.metatools.f2d.tools

import com.badlogic.gdx.math.Intersector.*
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.math.collision.Ray
import eu.metatools.f2d.capturable.Capturable
import eu.metatools.f2d.data.Vec

/**
 * A dedicated [Capturable] that checks if a sphere with a diameter of one intersects.
 */
object CaptureSphere : Capturable<Unit?> {
    override fun capture(args: Unit?, time: Double, origin: Vec, direction: Vec): Vec? {
        // Create  ray from vectors.
        val ray = Ray(origin.toVector(), direction.toVector())

        // Create output for intersection.
        val intersection = Vector3()

        // Intersect, return result if intersected.
        return if (intersectRaySphere(ray, Vector3.Zero, 0.5f, intersection))
            Vec(intersection)
        else
            null
    }
}

/**
 * A dedicated [Capturable] that checks if a cube with edges of length one intersects.
 */
object CaptureCube : Capturable<Unit?> {
    /**
     * The static bounding box to use for intersections.
     */
    private val boundingBox = BoundingBox(
        Vector3(-0.5f, -0.5f, -0.5f),
        Vector3(0.5f, 0.5f, 0.5f)
    )

    override fun capture(args: Unit?, time: Double, origin: Vec, direction: Vec): Vec? {
        // Create  ray from vectors.
        val ray = Ray(origin.toVector(), direction.toVector())

        // Create output for intersection.
        val intersection = Vector3()

        // Intersect, return result if intersected.
        return if (intersectRayBoundsFast(ray, boundingBox) && intersectRayBounds(ray, boundingBox, intersection))
            Vec(intersection)
        else
            null
    }
}