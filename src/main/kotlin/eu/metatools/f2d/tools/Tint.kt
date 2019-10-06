package eu.metatools.f2d.tools

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import eu.metatools.f2d.context.Drawable

/**
 * Runs the [Drawable]s [Drawable.draw] method with a different color.
 */
fun <T> Drawable<T>.tint(tint: Color) = object : Drawable<T> {
    override fun draw(args: T, time: Double, spriteBatch: SpriteBatch) {
        // Get original value.
        val colorBefore = spriteBatch.color.cpy()

        // Set new value, draw, then reset.
        spriteBatch.color = spriteBatch.color.mul(tint)
        this@tint.draw(args, time, spriteBatch)
        spriteBatch.color = colorBefore
    }
}