package eu.metatools.fio.data

import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch
import com.badlogic.gdx.graphics.g2d.SpriteBatch

/**
 * Blend mode values.
 */
data class Blend(val src: Int, val dst: Int, val srcAlpha: Int = src, val dstAlpha: Int = dst){
    companion object{
        val DEFAULT = Blend(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
    }
}

/**
 * Gets or sets the blend mode of the [SpriteBatch].
 */
var SpriteBatch.blend
    get() = Blend(blendSrcFunc, blendDstFunc, blendSrcFuncAlpha, blendDstFuncAlpha)
    set(value) = setBlendFunctionSeparate(
        value.src,
        value.dst,
        value.srcAlpha,
        value.dstAlpha
    )

/**
 * Gets or sets the blend mode of the [PolygonSpriteBatch].
 */
var PolygonSpriteBatch.blend
    get() = Blend(blendSrcFunc, blendDstFunc, blendSrcFuncAlpha, blendDstFuncAlpha)
    set(value) = setBlendFunctionSeparate(
        value.src,
        value.dst,
        value.srcAlpha,
        value.dstAlpha
    )
