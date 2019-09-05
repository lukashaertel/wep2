package eu.metatools.f2d.tools

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import eu.metatools.f2d.context.Drawable

/**
 * Runs the [Drawable]s [Drawable.generate] method with a different blend-function.
 */
fun <T> Drawable<T>.blend(src: Int, dst: Int, srcAlpha: Int? = null, dstAlpha: Int? = null) = object : Drawable<T> {
    override fun generate(args: T, time: Double, receiver: ((SpriteBatch) -> Unit) -> Unit) {
        receiver {
            // Get original values.
            val previousSrc = it.blendSrcFunc
            val previousDst = it.blendDstFunc
            val previousSrcAlpha = it.blendSrcFuncAlpha
            val previousDstAlpha = it.blendDstFuncAlpha

            // Set new values.
            it.setBlendFunctionSeparate(src, dst, srcAlpha ?: src, dstAlpha ?: dst)
            this@blend.generate(args, time, it::run)
            it.setBlendFunctionSeparate(previousSrc, previousDst, previousSrcAlpha, previousDstAlpha)
        }
    }
}