package eu.metatools.sx.ui

import com.badlogic.gdx.InputProcessor

class CaptureProcessor(val inputProcessor: InputProcessor, var handled: Boolean = false) : InputProcessor {
    override fun keyDown(keycode: Int): Boolean {
        if (!inputProcessor.keyDown(keycode))
            return false
        handled = true
        return true
    }

    override fun keyUp(keycode: Int): Boolean {
        if (!inputProcessor.keyUp(keycode))
            return false
        handled = true
        return true
    }

    override fun keyTyped(character: Char): Boolean {
        if (!inputProcessor.keyTyped(character))
            return false
        handled = true
        return true
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (!inputProcessor.touchDown(screenX, screenY, pointer, button))
            return false
        handled = true
        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (!inputProcessor.touchUp(screenX, screenY, pointer, button))
            return false
        handled = true
        return true
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        if (!inputProcessor.touchDragged(screenX, screenY, pointer))
            return false
        handled = true
        return true
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        if (!inputProcessor.mouseMoved(screenX, screenY))
            return false
        handled = true
        return true
    }

    override fun scrolled(amount: Int): Boolean {
        if (!inputProcessor.scrolled(amount))
            return false
        handled = true
        return true
    }

    fun reset() = handled.also {
        handled = false
    }
}