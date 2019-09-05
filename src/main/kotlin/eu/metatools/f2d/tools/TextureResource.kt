package eu.metatools.f2d.tools

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import eu.metatools.f2d.context.*

/**
 * A region that can turn a [Texture] into a [TextureRegion].
 */
sealed class Region {
    /**
     * Returns the texture region.
     */
    abstract fun apply(texture: Texture?): TextureRegion
}

/**
 * Returns the entire texture.
 */
object EntireRegion : Region() {
    override fun apply(texture: Texture?) =
        TextureRegion(texture)
}

/**
 * Returns an absolutely selected region.
 */
data class AbsoluteRegion(val x: Int, val y: Int, val width: Int, val height: Int) : Region() {
    override fun apply(texture: Texture?) =
        TextureRegion(texture, x, y, width, height)
}

/**
 * Returns a relatively selected region.
 */
data class UVRegion(val u: Float, val v: Float, val u2: Float, val v2: Float) : Region() {
    override fun apply(texture: Texture?) =
        TextureRegion(texture, u, v, u2, v2)
}

/**
 * Arguments for creating a [Drawable] from a [TextureResource].
 */
data class TextureResourceArgs(val region: Region = EntireRegion)

/**
 * Arguments for rendering a [Drawable] from a [TextureResource].
 */
data class TextureArgs(val tint: Color = Color.WHITE, val keepSize: Boolean = false)

/**
 * A texture resource on a file location.
 * @property location The location function.
 */
class TextureResource(
    val location: () -> FileHandle
) : NotifyingResource<TextureResourceArgs?, LifecycleDrawable<TextureArgs?>>() {

    companion object {
        /**
         * The default arguments to [refer].
         */
        val defaultArgsResource = TextureResourceArgs()

        /**
         * The default arguments to [Drawable.generate].
         */
        val defaultArgs = TextureArgs()
    }

    /**
     * The texture that is active.
     */
    private var texture: Texture? = null

    override fun initializeSelf() {
        if (texture == null)
            texture = Texture(location())

    }

    override fun disposeSelf() {
        texture?.dispose()
        texture = null
    }

    override fun referNew(argsResource: TextureResourceArgs?) = object : LifecycleDrawable<TextureArgs?> {
        val activeArgsResource = argsResource ?: defaultArgsResource

        var textureRegion: TextureRegion? = null

        override fun initialize() {
            if (textureRegion == null)
                textureRegion = activeArgsResource.region.apply(texture)
        }

        override fun dispose() {
            textureRegion = null
        }

        override fun generate(args: TextureArgs?, time: Double, receiver: ((SpriteBatch) -> Unit) -> Unit) {
            val activeArgs = args ?: defaultArgs

            val textureRegion = activeArgsResource.region.apply(texture)
            if (activeArgs.keepSize)
                receiver {
                    val source = it.color.cpy()
                    it.color = activeArgs.tint
                    it.draw(textureRegion, -(textureRegion.regionWidth / 2f), -(textureRegion.regionHeight / 2f))
                    it.color = source
                }
            else
                receiver {
                    val source = it.color.cpy()
                    it.color = activeArgs.tint
                    it.draw(textureRegion, -0.5f, -0.5f, 1f, 1f)
                    it.color = source
                }
        }
    }
}