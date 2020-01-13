package eu.metatools.f2d.context

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import com.badlogic.gdx.math.Matrix4
import eu.metatools.f2d.data.Blend
import eu.metatools.f2d.data.blend

/**
 * Standard mode-switching context. Parameters are passed as constructor arguments.
 */
class StandardContext(
    val spriteBatchSize: Int = 1000,
    val spriteBatchShaderProgram: ShaderProgram? = null,
    val polygonSpriteBatchSize: Int = 2000,
    val polygonSpriteBatchShaderProgram: ShaderProgram? = null,
    val shapeRendererMaxVertices: Int = 5000,
    val shapeRendererShaderProgram: ShaderProgram? = null
) : Context {
    /**
     * Modes of the context.
     */
    private enum class Mode {
        NONE,
        SPRITE_BATCH,
        POLYGON_SPRITE_BATCH,
        SHAPE_RENDERER
    }

    /**
     * True if [spriteBatch] is initialized.
     */
    private var spriteBatchInitialized = false

    /**
     * True if [polygonSpriteBatch] is initialized.
     */
    private var polygonSpriteBatchInitialized = false

    /**
     * True if [shapeRenderer] is initialized.
     */
    private var shapeRendererInitialized = false

    /**
     * Sprite batch, referring to this value will set [spriteBatchInitialized] to true.
     */
    private val spriteBatch by lazy {
        SpriteBatch(spriteBatchSize, spriteBatchShaderProgram).also {
            it.enableBlending()
            spriteBatchInitialized = true
        }
    }

    /**
     * Polygon sprite batch, referring to this value will set [polygonSpriteBatchInitialized] to true.
     */
    private val polygonSpriteBatch by lazy {
        PolygonSpriteBatch(
            polygonSpriteBatchSize,
            polygonSpriteBatchShaderProgram
        ).also {
            it.enableBlending()
            polygonSpriteBatchInitialized = true
        }
    }

    /**
     * Shape renderer, referring to this value will set [shapeRendererInitialized] to true.
     */
    private val shapeRenderer: ShapeRenderer by lazy {
        ShapeRenderer(
            shapeRendererMaxVertices,
            shapeRendererShaderProgram
        ).also {
            shapeRendererInitialized = true
        }
    }

    /**
     * Active mode.
     */
    private var mode = Mode.NONE

    /**
     * Color set on mode none.
     */
    private var colorValue: Color = Color.WHITE

    /**
     * Blend set on mode none or shapes.
     */
    private var blendValue: Blend = Blend.DEFAULT

    /**
     * Transformation set on mode none.
     */
    private var transformValue: Matrix4 = Matrix4().idt()

    /**
     * Projection set on mode none.
     */
    private var projectionValue: Matrix4 = Matrix4().idt()

    override var color: Color
        get() = when (mode) {
            Mode.NONE -> colorValue.cpy()
            Mode.SPRITE_BATCH -> spriteBatch.color.cpy()
            Mode.POLYGON_SPRITE_BATCH -> polygonSpriteBatch.color.cpy()
            Mode.SHAPE_RENDERER -> shapeRenderer.color.cpy()
        }
        set(value) {
            when (mode) {
                Mode.NONE -> colorValue.set(value)
                Mode.SPRITE_BATCH -> spriteBatch.color = value
                Mode.POLYGON_SPRITE_BATCH -> polygonSpriteBatch.color = value
                Mode.SHAPE_RENDERER -> shapeRenderer.color = value
            }
        }

    override var blend: Blend
        get() = when (mode) {
            Mode.NONE -> blendValue
            Mode.SPRITE_BATCH -> spriteBatch.blend
            Mode.POLYGON_SPRITE_BATCH -> polygonSpriteBatch.blend
            Mode.SHAPE_RENDERER -> blendValue
        }
        set(value) = when (mode) {
            Mode.NONE -> blendValue = value
            Mode.SPRITE_BATCH -> spriteBatch.blend = value
            Mode.POLYGON_SPRITE_BATCH -> polygonSpriteBatch.blend = value
            Mode.SHAPE_RENDERER -> blendValue = value
        }

    override var transform: Matrix4
        get() = when (mode) {
            Mode.NONE -> transformValue
            Mode.SPRITE_BATCH -> spriteBatch.transformMatrix
            Mode.POLYGON_SPRITE_BATCH -> polygonSpriteBatch.transformMatrix
            Mode.SHAPE_RENDERER -> shapeRenderer.transformMatrix
        }
        set(value) = when (mode) {
            Mode.NONE -> transformValue = value
            Mode.SPRITE_BATCH -> spriteBatch.transformMatrix = value
            Mode.POLYGON_SPRITE_BATCH -> polygonSpriteBatch.transformMatrix = value
            Mode.SHAPE_RENDERER -> shapeRenderer.transformMatrix = value
        }

    override var projection: Matrix4
        get() = when (mode) {
            Mode.NONE -> projectionValue
            Mode.SPRITE_BATCH -> spriteBatch.projectionMatrix
            Mode.POLYGON_SPRITE_BATCH -> polygonSpriteBatch.projectionMatrix
            Mode.SHAPE_RENDERER -> shapeRenderer.projectionMatrix
        }
        set(value) = when (mode) {
            Mode.NONE -> projectionValue = value
            Mode.SPRITE_BATCH -> spriteBatch.projectionMatrix = value
            Mode.POLYGON_SPRITE_BATCH -> polygonSpriteBatch.projectionMatrix = value
            Mode.SHAPE_RENDERER -> shapeRenderer.projectionMatrix = value
        }

    override fun sprites(): SpriteBatch {
        // Memorize active color and blend for  switch-over.
        val activeColor = color
        val activeBlend = blend
        val activeTransform = transform
        val activeProjection = projection

        // End any active mode, switch over or keep running.
        when (mode.also { mode = Mode.SPRITE_BATCH }) {
            Mode.NONE -> {
                spriteBatch.color = activeColor
                spriteBatch.blend = activeBlend
                spriteBatch.transformMatrix = activeTransform
                spriteBatch.projectionMatrix = activeProjection
                spriteBatch.begin()
                return spriteBatch
            }
            Mode.POLYGON_SPRITE_BATCH -> {
                polygonSpriteBatch.end()
                spriteBatch.color = activeColor
                spriteBatch.blend = activeBlend
                spriteBatch.transformMatrix = activeTransform
                spriteBatch.projectionMatrix = activeProjection
                spriteBatch.begin()
                return spriteBatch
            }
            Mode.SHAPE_RENDERER -> {
                shapeRenderer.end()
                spriteBatch.color = activeColor
                spriteBatch.blend = activeBlend
                spriteBatch.transformMatrix = activeTransform
                spriteBatch.projectionMatrix = activeProjection
                spriteBatch.begin()
                return spriteBatch
            }
            Mode.SPRITE_BATCH -> {
                return spriteBatch
            }
        }
    }

    override fun polygonSprites(): PolygonSpriteBatch {
        // Memorize active color and blend for switch-over.
        val activeColor = color
        val activeBlend = blend
        val activeTransform = transform
        val activeProjection = projection

        // End any active mode, switch over or keep running.
        when (mode.also { mode = Mode.POLYGON_SPRITE_BATCH }) {
            Mode.NONE -> {
                polygonSpriteBatch.color = activeColor
                polygonSpriteBatch.blend = activeBlend
                polygonSpriteBatch.transformMatrix = activeTransform
                polygonSpriteBatch.projectionMatrix = activeProjection
                polygonSpriteBatch.begin()
                return polygonSpriteBatch
            }
            Mode.SHAPE_RENDERER -> {
                shapeRenderer.end()
                polygonSpriteBatch.color = activeColor
                polygonSpriteBatch.blend = activeBlend
                polygonSpriteBatch.transformMatrix = activeTransform
                polygonSpriteBatch.projectionMatrix = activeProjection
                polygonSpriteBatch.begin()
                return polygonSpriteBatch
            }
            Mode.SPRITE_BATCH -> {
                spriteBatch.end()
                polygonSpriteBatch.color = activeColor
                polygonSpriteBatch.blend = activeBlend
                polygonSpriteBatch.transformMatrix = activeTransform
                polygonSpriteBatch.projectionMatrix = activeProjection
                polygonSpriteBatch.begin()
                return polygonSpriteBatch
            }
            Mode.POLYGON_SPRITE_BATCH -> {
                return polygonSpriteBatch
            }
        }
    }

    override fun shapes(shapeType: ShapeType): ShapeRenderer {
        // Memorize active color and blend for switch-over.
        val activeColor = color
        val activeBlend = blend
        val activeTransform = transform
        val activeProjection = projection

        // End any active mode, switch over or keep running.
        when (mode.also { mode = Mode.SHAPE_RENDERER }) {
            Mode.NONE -> {
                shapeRenderer.color = activeColor
                blendValue = activeBlend
                shapeRenderer.transformMatrix = activeTransform
                shapeRenderer.projectionMatrix = activeProjection
                shapeRenderer.begin(shapeType)
                return shapeRenderer
            }
            Mode.SPRITE_BATCH -> {
                spriteBatch.end()
                shapeRenderer.color = activeColor
                blendValue = activeBlend
                shapeRenderer.transformMatrix = activeTransform
                shapeRenderer.projectionMatrix = activeProjection
                shapeRenderer.begin(shapeType)
                return shapeRenderer
            }
            Mode.POLYGON_SPRITE_BATCH -> {
                polygonSpriteBatch.end()
                shapeRenderer.color = activeColor
                blendValue = activeBlend
                shapeRenderer.transformMatrix = activeTransform
                shapeRenderer.projectionMatrix = activeProjection
                shapeRenderer.begin(shapeType)
                return shapeRenderer
            }
            Mode.SHAPE_RENDERER -> {
                if (shapeRenderer.currentType != shapeType) {
                    shapeRenderer.end()
                    shapeRenderer.begin(shapeType)
                }
                return shapeRenderer
            }
        }
    }

    override fun none() {
        // Memorize active color and blend for switch-over.
        val activeColor = color
        val activeBlend = blend
        val activeTransform = transform
        val activeProjection = projection

        // End any active mode.
        when (mode.also { mode = Mode.NONE }) {
            Mode.SPRITE_BATCH -> {
                spriteBatch.end()
                colorValue = activeColor
                blendValue = activeBlend
                transformValue = activeTransform
                projectionValue = activeProjection
            }
            Mode.POLYGON_SPRITE_BATCH -> {
                polygonSpriteBatch.end()
                colorValue = activeColor
                blendValue = activeBlend
                transformValue = activeTransform
                projectionValue = activeProjection
            }
            Mode.SHAPE_RENDERER -> {
                shapeRenderer.end()
                colorValue = activeColor
                blendValue = activeBlend
                transformValue = activeTransform
                projectionValue = activeProjection
            }
            Mode.NONE -> {
            }
        }
    }

    override fun dispose() {
        // Dispose only initialized components.
        if (spriteBatchInitialized)
            spriteBatch.dispose()
        if (polygonSpriteBatchInitialized)
            polygonSpriteBatch.dispose()
        if (shapeRendererInitialized)
            shapeRenderer.dispose()
    }
}