package eu.metatools.ex.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import eu.metatools.f2d.util.centeredX
import eu.metatools.f2d.util.centeredY
import java.util.*

/**
 * Extended mouse button functionality.
 */
class Mouse {
    /**
     * Old mouse buttons from last frame.
     */
    private var wasDown = BitSet()

    /**
     * Current mouse buttons from this frame.
     */
    private var isDown = BitSet()

    /**
     * Set for returning.
     */
    private val returnSet = ThreadLocal.withInitial { BitSet() }

    /**
     * Center relative x coordinate.
     */
    val dx get() = Gdx.input.centeredX

    /**
     * Center relative y coordinate.
     */
    val dy get() = Gdx.input.centeredY

    /**
     * Updates the buffers.
     */
    fun fetch(): BitSet? {
        // Swap buffers.
        val swap = wasDown
        wasDown = isDown
        isDown = swap
        swap.clear()

        // Get return set for updating.
        val returnSet = returnSet.get()
        var hasChange = false

        // Update button states.
        for (button in Input.Buttons.LEFT..Input.Buttons.FORWARD) {
            isDown[button] = Gdx.input.isButtonPressed(button)
            if (isDown[button] != wasDown[button]) {
                hasChange = true
                returnSet[button] = true
            }
        }

        return returnSet.takeIf { hasChange }
    }

    /**
     * True if button is down.
     */
    fun pressed(button: Int) =
        isDown[button]

    /**
     * True if button is up.
     */
    fun released(button: Int) =
        !isDown[button]

    /**
     * True if button is just pressed.
     */
    fun justPressed(button: Int) =
        isDown[button] && !wasDown[button]

    /**
     * True if button is just released.
     */
    fun justReleased(button: Int) =
        !isDown[button] && wasDown[button]
}