package eu.metatools.f2d.tools

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import eu.metatools.f2d.context.Drawable
import eu.metatools.f2d.context.LifecycleResource

/**
 * Generates drawable resources with a given color. The results have the length one and are centered.
 */
class SolidResource : LifecycleResource<Unit, Drawable<Variation?>> {
    private var texture: Texture? = null

    override fun initialize() {
        // Check if already initialized.
        if (texture == null)
            texture = Texture(
                Pixmap(1, 1, Pixmap.Format.RGBA8888).apply {
                    setColor(Color.WHITE)
                    drawPixel(0, 0)
                })
    }

    override fun dispose() {
        texture?.dispose()
        texture = null
    }

    override fun refer(argsResource: Unit) =
        object : Drawable<Variation?> {
            override fun upload(args: Variation?, time: Double, receiver: ((SpriteBatch) -> Unit) -> Unit) {
                val activeArgs = args ?: Variation.DEFAULT

                // Keeping size has no effect, as it will result in the same call.
                receiver {
                    val source = it.color.cpy()
                    it.color = activeArgs.tint
                    it.draw(texture, -0.5f, -0.5f, 1f, 1f)
                    it.color = source
                }
            }
        }
}