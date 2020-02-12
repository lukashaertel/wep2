package eu.metatools.fio.context

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.Disposable
import eu.metatools.fio.data.Blend

/**
 * Context providing access to output operators [SpriteBatch], [PolygonSpriteBatch], [ShapeRenderer].
 */
interface Context : Disposable {
    /**
     * Color of the current mode.
     */
    var color: Color

    /**
     * Blend values of the current mode.
     */
    var blend: Blend

    /**
     * Transform value of the current mode.
     */
    var transform: Matrix4

    /**
     * Projection value of the current mode.
     */
    var projection: Matrix4

    /**
     * Get or activate [SpriteBatch].
     */
    fun sprites(): SpriteBatch

    /**
     * Get or activate [PolygonSpriteBatch].
     */
    fun polygonSprites(): PolygonSpriteBatch

    /**
     * Get or activate [ShapeRenderer].
     */
    fun shapes(shapeType: ShapeType): ShapeRenderer

    /**
     * Deactivate active contexts.
     */
    fun none()
}

