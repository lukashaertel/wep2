package eu.metatools.ex.input

import com.badlogic.gdx.Gdx
import eu.metatools.ex.data.Dir
import eu.metatools.f2d.util.centeredX
import eu.metatools.f2d.util.centeredY

/**
 * Computes looked-at direction from center.
 */
class Look {
    /**
     * Last output direction.
     */
    private var last: Dir? = null

    /**
     * Fetches the updated looked-at direction.
     */
    fun fetch(): Dir? {
        // Get aim direction.
        val dx = Gdx.input.centeredX
        val dy = Gdx.input.centeredY

        // Compute dir from direction and compare against last. If not equal, return change.
        val next = Dir.from(dx, dy)
        if (next != last) {
            last = next
            return next
        }

        return null
    }
}