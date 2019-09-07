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
object Entire : Region() {
    override fun apply(texture: Texture?) =
        TextureRegion(texture)
}

/**
 * Returns an absolutely selected region.
 */
data class Absolute(val x: Int, val y: Int, val width: Int, val height: Int) : Region() {
    override fun apply(texture: Texture?) =
        TextureRegion(texture, x, y, width, height)
}

/**
 * Returns a relatively selected region.
 */
data class UV(val u: Float, val v: Float, val u2: Float, val v2: Float) : Region() {
    override fun apply(texture: Texture?) =
        TextureRegion(texture, u, v, u2, v2)
}

/**
 * Arguments to refer a [TextureResource].
 */
data class ReferTexture(val region: Region = DEFAULT.region) {
    companion object {
        /**
         * The default value of referring to a texture.
         */
        val DEFAULT = ReferTexture(Entire)
    }
}

/**
 * Variation of how to draw something to a sprite batch.
 */
data class Variation(val tint: Color = DEFAULT.tint, val keepSize: Boolean = DEFAULT.keepSize) {
    companion object {
        /**
         * The default variation.
         */
        val DEFAULT = Variation(Color.WHITE, false)
    }
}

/**
 * A texture resource on a file location.
 * @property location The location function.
 */
class TextureResource(
    val location: () -> FileHandle
) : NotifyingResource<ReferTexture?, LifecycleDrawable<Variation?>>() {

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

    override fun referNew(argsResource: ReferTexture?) = object : LifecycleDrawable<Variation?> {
        val activeArgsResource = argsResource ?: ReferTexture.DEFAULT

        var textureRegion: TextureRegion? = null

        override fun initialize() {
            if (textureRegion == null)
                textureRegion = activeArgsResource.region.apply(texture)
        }

        override fun dispose() {
            textureRegion = null
        }

        override fun upload(args: Variation?, time: Double, receiver: ((SpriteBatch) -> Unit) -> Unit) {
            val activeArgs = args ?: Variation.DEFAULT

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