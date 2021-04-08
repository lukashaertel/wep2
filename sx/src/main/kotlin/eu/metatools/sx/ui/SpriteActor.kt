package eu.metatools.sx.ui

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.utils.TransformDrawable

class SpriteActor : Actor() {
    var drawable: TransformDrawable? = null

    override fun draw(batch: Batch, parentAlpha: Float) {
        drawable?.draw(batch, x, y, originX, originY, width, height, scaleX, scaleY, rotation)
    }
}