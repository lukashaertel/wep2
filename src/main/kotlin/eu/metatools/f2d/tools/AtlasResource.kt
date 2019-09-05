package eu.metatools.f2d.tools

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import eu.metatools.f2d.context.*
import java.lang.IllegalArgumentException

/**
 * An abstract texture atlas resource on a file location.
 * @property location The location function.
 */
abstract class AtlasResource<A, I : LifecycleDrawable<*>>(
    val location: () -> FileHandle
) : NotifyingResource<A, I>() {
    protected var textureAtlas: TextureAtlas? = null

    override fun initializeSelf() {
        if (textureAtlas == null)
            textureAtlas = TextureAtlas(location())
    }

    override fun disposeSelf() {
        textureAtlas?.dispose()
        textureAtlas = null
    }
}

/**
 * Arguments to create an animated atlas resource.
 */
data class AnimatedAtlasResourceArgs(val name: String, val length: Double)

/**
 * Uses a texture atlas to create a set of animated drawables.
 * @property location The location function.
 */
class AnimatedAtlasResource(
    location: () -> FileHandle
) : AtlasResource<AnimatedAtlasResourceArgs, LifecycleDrawable<TextureArgs?>>(location) {
    companion object {
        /**
         * The default arguments to [Drawable.generate].
         */
        val defaultArgs = TextureArgs()
    }

    override fun referNew(argsResource: AnimatedAtlasResourceArgs) = object : LifecycleDrawable<TextureArgs?> {
        var regions: List<TextureAtlas.AtlasRegion>? = null

        override fun initialize() {
            // Find regions, see that they are not null.
            regions = textureAtlas?.findRegions(argsResource.name)?.toList()
                ?: throw IllegalArgumentException("Unable to load ${argsResource.name}")

            // Check that not empty as well.
            check(regions?.size ?: 0 > 0) {
                "Unable to load ${argsResource.name}, empty regions returned."
            }
        }

        override fun dispose() {
            regions = null
        }

        override fun generate(args: TextureArgs?, time: Double, receiver: ((SpriteBatch) -> Unit) -> Unit) {
            val activeArgs = args ?: defaultArgs

            regions?.let { regions ->
                val regionIndex = (time / (argsResource.length / regions.size)).toInt() % regions.size
                val region = regions[regionIndex]

                if (activeArgs.keepSize)
                    receiver {
                        val source = it.color.cpy()
                        it.color = activeArgs.tint
                        it.draw(region, -(region.regionWidth / 2f), -(region.regionHeight / 2f))
                        it.color = source
                    }
                else
                    receiver {
                        val source = it.color.cpy()
                        it.color = activeArgs.tint
                        it.draw(region, -0.5f, -0.5f, 1.0f, 1.0f)
                        it.color = source
                    }
            }
        }
    }

}