package eu.metatools.sx.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.utils.TransformDrawable
import eu.metatools.reaktor.gdx.VActor

class VSpriteActor(
        val drawable: TransformDrawable? = defaultDrawable,
        color: Color = defaultColor,
        name: String? = defaultName,
        originX: Float = defaultOriginX,
        originY: Float = defaultOriginY,
        x: Float = defaultX,
        y: Float = defaultY,
        width: Float = defaultWidth,
        height: Float = defaultHeight,
        rotation: Float = defaultRotation,
        scaleX: Float = defaultScaleX,
        scaleY: Float = defaultScaleY,
        visible: Boolean = defaultVisible,
        debug: Boolean = defaultDebug,
        touchable: Touchable = defaultTouchable,
        listeners: List<EventListener> = defaultListeners,
        captureListeners: List<EventListener> = defaultCaptureListeners,
        ref: (SpriteActor) -> Unit = defaultRef,
) : VActor<SpriteActor>(
        color,
        name,
        originX,
        originY,
        x,
        y,
        width,
        height,
        rotation,
        scaleX,
        scaleY,
        visible,
        debug,
        touchable,
        listeners,
        captureListeners,
        ref) {
    companion object {
        val defaultDrawable: TransformDrawable? = null
        private const val ownProps = 1
    }

    override fun create() = SpriteActor()

    override fun assign(actual: SpriteActor) {
        actual.drawable = drawable
        super.assign(actual)
    }

    override val props = ownProps + super.props

    override fun getOwn(prop: Int) = when (prop) {
        0 -> drawable
        else -> super.getOwn(prop - ownProps)
    }

    override fun getActual(prop: Int, actual: SpriteActor): Any? = when (prop) {
        0 -> actual.drawable
        else -> super.getActual(prop - ownProps, actual)
    }

    override fun updateActual(prop: Int, actual: SpriteActor, value: Any?) {
        when (prop) {
            0 -> actual.drawable = value as TransformDrawable?
            else -> super.updateActual(prop - ownProps, actual, value)
        }
    }
}