package eu.metatools.ex.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import eu.metatools.f2d.math.Cell

fun Boolean.toInt() = if (this) 1 else 0

/**
 * Generates a [Pt] from the pressed state of the given keys.
 */
class KeyStick(
    val left: Int = Input.Keys.A,
    val up: Int = Input.Keys.W,
    val right: Int = Input.Keys.D,
    val down: Int = Input.Keys.S
) {
    /**
     * The last value or null.
     */
    private var last: Cell? = null

    /**
     * Gets the changed value or null if unchanged.
     */
    fun fetch(): Cell? {
        val dx = Gdx.input.isKeyPressed(right).toInt() -
                Gdx.input.isKeyPressed(left).toInt()
        val dy = Gdx.input.isKeyPressed(up).toInt() -
                Gdx.input.isKeyPressed(down).toInt()

        val next = Cell(dx, dy)
        if (next != last) {
            last = next
            return next
        }

        return null
    }

}