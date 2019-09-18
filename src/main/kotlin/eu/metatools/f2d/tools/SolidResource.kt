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
            override fun draw(args: Variation?, time: Double, spriteBatch: SpriteBatch) {
                // Get texture or return if not assigned yet.
                val texture = texture ?: return

                // Get args or default.
                val activeArgs = args ?: Variation.DEFAULT

                // Memorize color.
                val colorBefore = spriteBatch.color.cpy()
                spriteBatch.color = activeArgs.tint

                // Draw, sizing is irrelevant as solid resources have the same size as uniform draws.
                spriteBatch.draw(texture, -0.5f, -0.5f, 1.0f, 1.0f)

                // Reset color.
                spriteBatch.color = colorBefore
            }
        }
}