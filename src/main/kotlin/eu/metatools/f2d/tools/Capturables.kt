package eu.metatools.f2d.tools

import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.math.collision.Ray
import eu.metatools.f2d.context.Capturable

object Sphere : Capturable<Unit?> {
    override fun upload(args: Unit?, time: Double, receiver: ((Ray, Vector3) -> Boolean) -> Unit) {
        receiver { ray, intersection ->
            Intersector.intersectRaySphere(ray, Vector3.Zero, 0.5f, intersection)
        }
    }
}

object Cube : Capturable<Unit?> {
    private val boundingBox = BoundingBox(
        Vector3(-0.5f, -0.5f, -0.5f),
        Vector3(0.5f, 0.5f, 0.5f)
    )

    override fun upload(args: Unit?, time: Double, receiver: ((Ray, Vector3) -> Boolean) -> Unit) {
        receiver { ray, intersection ->
            if (Intersector.intersectRayBoundsFast(ray, boundingBox))
                Intersector.intersectRayBounds(ray, boundingBox, intersection)
            else
                false
        }
    }
}