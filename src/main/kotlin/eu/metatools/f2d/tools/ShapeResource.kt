package eu.metatools.f2d.tools

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import eu.metatools.f2d.context.Drawable
import eu.metatools.f2d.context.LifecycleResource
import eu.metatools.f2d.context.MemorizingResource
import eu.metatools.f2d.context.Resource
import eu.metatools.f2d.math.Pt
import eu.metatools.f2d.math.Pts

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
class ShapeResource : LifecycleResource<ReferShape, Drawable<Unit?>> {
    private var shapeRenderer: ShapeRenderer? = null

    override fun initialize() {
        if (shapeRenderer == null)
            shapeRenderer = ShapeRenderer()
    }

    override fun dispose() {
        shapeRenderer?.dispose()
        shapeRenderer = null
    }

    override fun get(argsResource: ReferShape) =
        when (argsResource) {
            is ReferLine -> object : Drawable<Unit?> {
                override fun draw(args: Unit?, time: Double, spriteBatch: SpriteBatch) {
                    shapeRenderer?.let {
                        spriteBatch.end()
                        it.projectionMatrix = spriteBatch.projectionMatrix
                        it.transformMatrix = spriteBatch.transformMatrix
                        it.color = spriteBatch.color

                        it.begin(argsResource.shapeType)
                        it.line(
                            argsResource.from.x,
                            argsResource.from.y,
                            argsResource.to.x,
                            argsResource.to.y
                        )
                        it.end()
                        spriteBatch.begin()
                    }
                }
            }
            is ReferPoly -> object : Drawable<Unit?> {
                override fun draw(args: Unit?, time: Double, spriteBatch: SpriteBatch) {
                    shapeRenderer?.let {
                        spriteBatch.end()
                        it.projectionMatrix = spriteBatch.projectionMatrix
                        it.transformMatrix = spriteBatch.transformMatrix
                        it.color = spriteBatch.color

                        it.begin(argsResource.shapeType)
                        it.polygon(argsResource.points.values)
                        it.end()
                        spriteBatch.begin()
                    }
                }
            }

            is ReferBox -> object : Drawable<Unit?> {
                override fun draw(args: Unit?, time: Double, spriteBatch: SpriteBatch) {
                    shapeRenderer?.let {
                        spriteBatch.end()
                        it.projectionMatrix = spriteBatch.projectionMatrix
                        it.transformMatrix = spriteBatch.transformMatrix
                        it.begin(argsResource.shapeType)
                        it.rect(
                            argsResource.from.x,
                            argsResource.from.y,
                            argsResource.to.x,
                            argsResource.to.y
                        )
                        it.end()
                        spriteBatch.begin()
                    }
                }
            }
        }


}