package eu.metatools.f2d.tools

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import eu.metatools.f2d.context.Drawable

/**
 * Runs the [Drawable]s [Drawable.draw] method with a different blend-function.
 */
fun <T> Drawable<T>.blend(src: Int, dst: Int, srcAlpha: Int? = null, dstAlpha: Int? = null) = object : Drawable<T> {
    override fun draw(args: T, time: Double, spriteBatch: SpriteBatch) {
        // Get original values.
        val previousSrc = spriteBatch.blendSrcFunc
        val previousDst = spriteBatch.blendDstFunc
        val previousSrcAlpha = spriteBatch.blendSrcFuncAlpha
        val previousDstAlpha = spriteBatch.blendDstFuncAlpha

        // Set new values, draw, then reset.
        spriteBatch.setBlendFunctionSeparate(src, dst, srcAlpha ?: src, dstAlpha ?: dst)
        this@blend.draw(args, time, spriteBatch)
        spriteBatch.setBlendFunctionSeparate(previousSrc, previousDst, previousSrcAlpha, previousDstAlpha)
    }
}