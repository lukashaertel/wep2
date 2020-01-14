package eu.metatools.ex.data

import com.badlogic.gdx.math.MathUtils
import kotlin.math.atan2

/**
 * Four way direction.
 */
enum class Dir {
    Up, Right, Left, Down;

    /**
     * Selects the appropriate value with a `when` block.
     */
    fun <T> select(up: T, right: T, down: T, left: T) = when (this) {
        Up -> up
        Right -> right
        Down -> down
        Left -> left
    }

    companion object {
        /**
         * Range of detecting as [Right].
         */
        private val rightRange = -45f..45f

        /**
         * Range of detecting as [Up].
         */
        private val upRange = 45f..135f

        /**
         * Range of detecting as [Down].
         */
        private val downRange = -135f..-45f

        /**
         * Computes the direction from quadrants.
         */
        fun from(x: Number, y: Number): Dir? {
            val xf = x.toFloat()
            val yf = y.toFloat()
            if (xf == 0.0f && yf == 0.0f)
                return null
            return when (MathUtils.radiansToDegrees * atan2(yf, xf)) {
                in rightRange -> Right
                in upRange -> Up
                in downRange -> Down
                else -> Left
            }
        }
    }
}