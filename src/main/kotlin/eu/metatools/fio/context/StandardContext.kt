package eu.metatools.fio.context

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import com.badlogic.gdx.math.Matrix4
import eu.metatools.fio.data.Blend
import eu.metatools.fio.data.blend

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
    private sealed class Mode {
        object None : Mode()
        object SpriteBatch : Mode()
        object PolygonSpriteBatch : Mode()
        object ShapeRenderer : Mode()
        data class Shader(
            val shaderProgram: ShaderProgram,
            val transform: String?,
            val projection: String?,
            val color: String?
        ) : Mode()
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

    private var shaderProgram: ShaderProgram? = null
    private var locationTransform: Int? = null
    private var locationProjection: Int? = null
    private var locationColor: Int? = null

    /**
     * Active mode.
     */
    private var mode: Mode = Mode.None

    /**
     * Color set on mode none.
     */
    private var colorValue: Color = Color.WHITE.cpy() // TODO: GDX color system is super fucked.

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
            Mode.None -> colorValue.cpy()
            Mode.SpriteBatch -> spriteBatch.color.cpy()
            Mode.PolygonSpriteBatch -> polygonSpriteBatch.color.cpy()
            Mode.ShapeRenderer -> shapeRenderer.color.cpy()
            is Mode.Shader -> colorValue.cpy()
        }
        set(value) {
            when (mode) {
                Mode.None -> colorValue.set(value)
                Mode.SpriteBatch -> spriteBatch.color = value
                Mode.PolygonSpriteBatch -> polygonSpriteBatch.color = value
                Mode.ShapeRenderer -> shapeRenderer.color = value
                is Mode.Shader -> {
                    colorValue.set(value)
                    shaderProgram?.setUniformf(locationColor ?: return, value)
                }
            }
        }

    override var blend: Blend
        get() = when (mode) {
            Mode.None -> blendValue
            Mode.SpriteBatch -> spriteBatch.blend
            Mode.PolygonSpriteBatch -> polygonSpriteBatch.blend
            Mode.ShapeRenderer -> blendValue
            is Mode.Shader -> blendValue
        }
        set(value) {
            when (mode) {
                Mode.None -> blendValue = value
                Mode.SpriteBatch -> spriteBatch.blend = value
                Mode.PolygonSpriteBatch -> polygonSpriteBatch.blend = value
                Mode.ShapeRenderer -> blendValue = value
                is Mode.Shader -> blendValue = value
            }
        }

    override var transform: Matrix4
        get() = when (mode) {
            Mode.None -> transformValue
            Mode.SpriteBatch -> spriteBatch.transformMatrix
            Mode.PolygonSpriteBatch -> polygonSpriteBatch.transformMatrix
            Mode.ShapeRenderer -> shapeRenderer.transformMatrix
            is Mode.Shader -> transformValue
        }
        set(value) {
            when (mode) {
                Mode.None -> transformValue = value
                Mode.SpriteBatch -> spriteBatch.transformMatrix = value
                Mode.PolygonSpriteBatch -> polygonSpriteBatch.transformMatrix = value
                Mode.ShapeRenderer -> shapeRenderer.transformMatrix = value
                is Mode.Shader -> {
                    transformValue = value
                    shaderProgram?.setUniformMatrix(locationTransform ?: return, value)
                }
            }
        }

    override var projection: Matrix4
        get() = when (mode) {
            Mode.None -> projectionValue
            Mode.SpriteBatch -> spriteBatch.projectionMatrix
            Mode.PolygonSpriteBatch -> polygonSpriteBatch.projectionMatrix
            Mode.ShapeRenderer -> shapeRenderer.projectionMatrix
            is Mode.Shader -> projectionValue
        }
        set(value) {
            when (mode) {
                Mode.None -> projectionValue = value
                Mode.SpriteBatch -> spriteBatch.projectionMatrix = value
                Mode.PolygonSpriteBatch -> polygonSpriteBatch.projectionMatrix = value
                Mode.ShapeRenderer -> shapeRenderer.projectionMatrix = value
                is Mode.Shader -> {
                    projectionValue = value
                    shaderProgram?.setUniformMatrix(locationProjection ?: return, value)
                }
            }
        }

    override fun sprites(): SpriteBatch {
        // Memorize active color and blend for  switch-over.
        val activeColor = color
        val activeBlend = blend
        val activeTransform = transform
        val activeProjection = projection

        // End any active mode, switch over or keep running.
        when (mode.also { mode = Mode.SpriteBatch }) {
            Mode.None -> {
                spriteBatch.color = activeColor
                spriteBatch.blend = activeBlend
                spriteBatch.transformMatrix = activeTransform
                spriteBatch.projectionMatrix = activeProjection
                spriteBatch.begin()
                return spriteBatch
            }
            Mode.PolygonSpriteBatch -> {
                polygonSpriteBatch.end()
                spriteBatch.color = activeColor
                spriteBatch.blend = activeBlend
                spriteBatch.transformMatrix = activeTransform
                spriteBatch.projectionMatrix = activeProjection
                spriteBatch.begin()
                return spriteBatch
            }
            Mode.ShapeRenderer -> {
                shapeRenderer.end()
                spriteBatch.color = activeColor
                spriteBatch.blend = activeBlend
                spriteBatch.transformMatrix = activeTransform
                spriteBatch.projectionMatrix = activeProjection
                spriteBatch.begin()
                return spriteBatch
            }
            is Mode.Shader -> {
                shaderProgram?.end()
                shaderProgram = null
                spriteBatch.color = activeColor
                spriteBatch.blend = activeBlend
                spriteBatch.transformMatrix = activeTransform
                spriteBatch.projectionMatrix = activeProjection
                spriteBatch.begin()
                return spriteBatch
            }
            Mode.SpriteBatch -> {
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
        when (mode.also { mode = Mode.PolygonSpriteBatch }) {
            Mode.None -> {
                polygonSpriteBatch.color = activeColor
                polygonSpriteBatch.blend = activeBlend
                polygonSpriteBatch.transformMatrix = activeTransform
                polygonSpriteBatch.projectionMatrix = activeProjection
                polygonSpriteBatch.begin()
                return polygonSpriteBatch
            }
            Mode.ShapeRenderer -> {
                shapeRenderer.end()
                polygonSpriteBatch.color = activeColor
                polygonSpriteBatch.blend = activeBlend
                polygonSpriteBatch.transformMatrix = activeTransform
                polygonSpriteBatch.projectionMatrix = activeProjection
                polygonSpriteBatch.begin()
                return polygonSpriteBatch
            }
            Mode.SpriteBatch -> {
                spriteBatch.end()
                polygonSpriteBatch.color = activeColor
                polygonSpriteBatch.blend = activeBlend
                polygonSpriteBatch.transformMatrix = activeTransform
                polygonSpriteBatch.projectionMatrix = activeProjection
                polygonSpriteBatch.begin()
                return polygonSpriteBatch
            }
            is Mode.Shader -> {
                shaderProgram?.end()
                shaderProgram = null
                polygonSpriteBatch.color = activeColor
                polygonSpriteBatch.blend = activeBlend
                polygonSpriteBatch.transformMatrix = activeTransform
                polygonSpriteBatch.projectionMatrix = activeProjection
                polygonSpriteBatch.begin()
                return polygonSpriteBatch
            }
            Mode.PolygonSpriteBatch -> {
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
        when (mode.also { mode = Mode.ShapeRenderer }) {
            Mode.None -> {
                shapeRenderer.color = activeColor
                blendValue = activeBlend
                shapeRenderer.transformMatrix = activeTransform
                shapeRenderer.projectionMatrix = activeProjection
                shapeRenderer.begin(shapeType)
                return shapeRenderer
            }
            Mode.SpriteBatch -> {
                spriteBatch.end()
                shapeRenderer.color = activeColor
                blendValue = activeBlend
                shapeRenderer.transformMatrix = activeTransform
                shapeRenderer.projectionMatrix = activeProjection
                shapeRenderer.begin(shapeType)
                return shapeRenderer
            }
            Mode.PolygonSpriteBatch -> {
                polygonSpriteBatch.end()
                shapeRenderer.color = activeColor
                blendValue = activeBlend
                shapeRenderer.transformMatrix = activeTransform
                shapeRenderer.projectionMatrix = activeProjection
                shapeRenderer.begin(shapeType)
                return shapeRenderer
            }
            is Mode.Shader -> {
                shaderProgram?.end()
                shaderProgram = null
                shapeRenderer.color = activeColor
                blendValue = activeBlend
                shapeRenderer.transformMatrix = activeTransform
                shapeRenderer.projectionMatrix = activeProjection
                shapeRenderer.begin(shapeType)
                return shapeRenderer
            }
            Mode.ShapeRenderer -> {
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
        when (mode.also { mode = Mode.None }) {
            Mode.SpriteBatch -> {
                spriteBatch.end()
                colorValue = activeColor
                blendValue = activeBlend
                transformValue = activeTransform
                projectionValue = activeProjection
            }
            Mode.PolygonSpriteBatch -> {
                polygonSpriteBatch.end()
                colorValue = activeColor
                blendValue = activeBlend
                transformValue = activeTransform
                projectionValue = activeProjection
            }
            Mode.ShapeRenderer -> {
                shapeRenderer.end()
                colorValue = activeColor
                blendValue = activeBlend
                transformValue = activeTransform
                projectionValue = activeProjection
            }
            is Mode.Shader -> {
                shaderProgram?.end()
                shaderProgram = null
                colorValue = activeColor
                blendValue = activeBlend
                transformValue = activeTransform
                projectionValue = activeProjection
            }
            Mode.None -> {
            }
        }
    }

    override fun shader(
        activate: ShaderProgram,
        uniformTransform: String?,
        uniformProjection: String?,
        uniformColor: String?
    ) {
        // Memorize active color and blend for switch-over.
        val activeColor = color
        val activeBlend = blend
        val activeTransform = transform
        val activeProjection = projection

        // Replace with shader mode.
        when (mode.also { mode = Mode.Shader(activate, uniformTransform, uniformProjection, uniformColor) }) {
            Mode.SpriteBatch -> {
                spriteBatch.end()
                locationColor = uniformColor?.let { activate.getUniformLocation(uniformColor) } ?: -1
                locationTransform = uniformTransform?.let { activate.getUniformLocation(uniformTransform) } ?: -1
                locationProjection = uniformProjection?.let { activate.getUniformLocation(uniformProjection) } ?: -1
            }
            Mode.PolygonSpriteBatch -> {
                polygonSpriteBatch.end()
                locationColor = uniformColor?.let { activate.getUniformLocation(uniformColor) } ?: -1
                locationTransform = uniformTransform?.let { activate.getUniformLocation(uniformTransform) } ?: -1
                locationProjection = uniformProjection?.let { activate.getUniformLocation(uniformProjection) } ?: -1
            }
            Mode.ShapeRenderer -> {
                shapeRenderer.end()
                locationColor = uniformColor?.let { activate.getUniformLocation(uniformColor) } ?: -1
                locationTransform = uniformTransform?.let { activate.getUniformLocation(uniformTransform) } ?: -1
                locationProjection = uniformProjection?.let { activate.getUniformLocation(uniformProjection) } ?: -1
            }
            is Mode.Shader -> {
                if (shaderProgram != activate) {
                    shaderProgram?.end()
                    locationColor = uniformColor?.let { activate.getUniformLocation(uniformColor) } ?: -1
                    locationTransform = uniformTransform?.let { activate.getUniformLocation(uniformTransform) } ?: -1
                    locationProjection = uniformProjection?.let { activate.getUniformLocation(uniformProjection) } ?: -1
                }
            }
            Mode.None -> {
                locationColor = uniformColor?.let { activate.getUniformLocation(uniformColor) } ?: -1
                locationTransform = uniformTransform?.let { activate.getUniformLocation(uniformTransform) } ?: -1
                locationProjection = uniformProjection?.let { activate.getUniformLocation(uniformProjection) } ?: -1
            }
        }

        shaderProgram = activate
        activate.begin()
        locationColor?.let { activate.setUniformf(it, activeColor) }
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFuncSeparate(activeBlend.src, activeBlend.dst, activeBlend.srcAlpha, activeBlend.dstAlpha)
        locationTransform?.let { activate.setUniformMatrix(it, activeTransform) }
        locationProjection?.let { activate.setUniformMatrix(it, activeProjection) }

        colorValue = activeColor
        blendValue = activeBlend
        transformValue = activeTransform
        projectionValue = activeProjection
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