package eu.metatools.f2d.tools

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import eu.metatools.f2d.context.Context
import eu.metatools.f2d.resource.LifecycleDrawable
import eu.metatools.f2d.resource.NotifyingResource

/**
 * Arguments to refer an [AtlasResource].
 */
sealed class ReferAtlas

/**
 * A static atlas referral.
 * @property name The name of the atlas region.
 */
data class Static(val name: String) : ReferAtlas()

/**
 * An animated atlas referral.
 * @property name The name of the atlas regions.
 * @property length The length of the animation.
 * @property looping True if looping.
 */
data class Animated(val name: String, val length: Double, val looping: Boolean = true) : ReferAtlas()

/**
 * An abstract texture atlas resource on a file location.
 * @property location The location function.
 */
class AtlasResource(
    val location: () -> FileHandle
) : NotifyingResource<ReferAtlas, LifecycleDrawable<Unit?>>() {
    /**
     * The atlas the resources are taken from when referring a new drawable.
     */
    private var textureAtlas: TextureAtlas? = null

    override fun initializeSelf() {
        // Initialize atlas if not already initialized.
        if (textureAtlas == null)
            textureAtlas = TextureAtlas(location())
    }

    override fun disposeSelf() {
        // Dispose atlas if initialized.
        textureAtlas?.dispose()
        textureAtlas = null
    }

    override fun referNew(argsResource: ReferAtlas) = when (argsResource) {
        // Static drawable is requested.
        is Static -> object : LifecycleDrawable<Unit?> {
            /**
             * The region to draw when initialized.
             */
            var region: TextureAtlas.AtlasRegion? = null

            override fun initialize() {
                // Find region, see that it is not null.
                region = textureAtlas?.findRegion(argsResource.name)
                    ?: throw IllegalArgumentException("Unable to load ${argsResource.name}")
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

            override val duration: Double
                get() =
                    // Duration is always infinite.
                    Double.POSITIVE_INFINITY
        }

        // Animated drawable is requested.
        is Animated -> object : LifecycleDrawable<Unit?> {
            /**
             * The regions to draw when initialized.
             */
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

            override fun draw(args: Unit?, time: Double, context: Context) {
                // Get regions or return if not assigned yet.
                val regions = regions ?: return

                val region = (time / (argsResource.length / regions.size)).toInt().let {
                    // If looping, wrap by modulo, otherwise stop at last index.
                    if (argsResource.looping)
                        regions[it % regions.size]
                    else
                        regions[minOf(it, regions.size - 1)]
                }

                // Draw to sprite batch.
                context.sprites().draw(region, -0.5f, -0.5f, 1.0f, 1.0f)
            }

            override val duration: Double
                get() =
                    // Duration is infinite if looping, otherwise explicitly given.
                    if (argsResource.looping)
                        Double.POSITIVE_INFINITY
                    else
                        argsResource.length
        }
    }
}