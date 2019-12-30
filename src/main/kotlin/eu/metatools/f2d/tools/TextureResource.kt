package eu.metatools.f2d.tools

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import eu.metatools.f2d.context.Context
import eu.metatools.f2d.resource.LifecycleDrawable
import eu.metatools.f2d.resource.NotifyingResource

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
 * A texture resource on a file location.
 * @property location The location function.
 */
class TextureResource(
    val location: () -> FileHandle
) : NotifyingResource<ReferTexture?, LifecycleDrawable<Unit?>>() {

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

    override fun referNew(argsResource: ReferTexture?) = object : LifecycleDrawable<Unit?> {
        val activeArgsResource = argsResource ?: ReferTexture.DEFAULT

        var region: TextureRegion? = null

        override fun initialize() {
            if (region == null)
                region = activeArgsResource.region.apply(texture)
        }

        override fun dispose() {
            region = null
        }

        override fun draw(args: Unit?, time: Double, context: Context) {
            // Get region or return if not assigned yet.
            val region = region ?: return

            // Draw to sprite batch.
            context.sprites().draw(region, -0.5f, -0.5f, 1.0f, 1.0f)
        }
    }
}