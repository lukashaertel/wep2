package eu.metatools.f2d.tools

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import eu.metatools.f2d.context.Drawable
import eu.metatools.f2d.context.Resource

/**
 * Generates drawable resources with a given color. The results have the length one and are centered.
 */
class SolidResource : Resource<Drawable<Color>> {
    var texture: Texture? = null

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

    override fun refer() =
        object : Drawable<Color> {
            override fun generate(args: Color, time: Double, receiver: ((SpriteBatch) -> Unit) -> Unit) {
                receiver {
                    val source = it.color.cpy()
                    it.color = args
                    it.draw(texture, -0.5f, -0.5f)
                    it.color = source
                }
            }

            override fun hasStarted(args: Color, time: Double) =
                0.0 <= time //TODO Figure out a standard.

            override fun hasEnded(args: Color, time: Double) =
                false
        }
}