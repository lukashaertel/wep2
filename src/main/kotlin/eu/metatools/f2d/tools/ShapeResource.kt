package eu.metatools.f2d.tools

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import eu.metatools.f2d.context.Context
import eu.metatools.f2d.data.Pt
import eu.metatools.f2d.data.Pts
import eu.metatools.f2d.drawable.Drawable
import eu.metatools.f2d.resource.Resource

sealed class ReferShape {
    abstract val filled: Boolean
}

data class ReferLine(val from: Pt, val to: Pt) : ReferShape() {
    override val filled: Boolean
        get() = false
}

data class ReferPoly(val points: Pts) : ReferShape() {
    override val filled: Boolean
        get() = false
}

data class ReferBox(override val filled: Boolean, val from: Pt, val to: Pt) : ReferShape()

private val ReferShape.shapeType
    get() = if (filled) ShapeRenderer.ShapeType.Filled
    else
        ShapeRenderer.ShapeType.Line

/**
 * Generates drawable resources with a given color. The results have the length one and are centered.
 */
object ShapeResource : Resource<ReferShape, Drawable<Unit?>> {
    override fun get(argsResource: ReferShape) =
        when (argsResource) {
            is ReferLine -> object : Drawable<Unit?> {
                override fun draw(args: Unit?, time: Double, context: Context) {
                    val shapes = context.shapes(argsResource.shapeType)
                    shapes.line(
                        argsResource.from.x,
                        argsResource.from.y,
                        argsResource.to.x,
                        argsResource.to.y
                    )
                }
            }
            is ReferPoly -> object : Drawable<Unit?> {
                override fun draw(args: Unit?, time: Double, context: Context) {
                    val shapes = context.shapes(argsResource.shapeType)
                    shapes.polygon(argsResource.points.values)
                }
            }

            is ReferBox -> object : Drawable<Unit?> {
                override fun draw(args: Unit?, time: Double, context: Context) {
                    val shapes = context.shapes(argsResource.shapeType)
                    shapes.rect(
                        argsResource.from.x,
                        argsResource.from.y,
                        argsResource.to.x,
                        argsResource.to.y
                    )
                }
            }
        }


}