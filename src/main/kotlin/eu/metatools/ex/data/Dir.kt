package eu.metatools.ex.data

import com.badlogic.gdx.math.MathUtils
import kotlin.math.atan2

enum class Dir {
    Up, Right, Left, Down;

    fun <T> select(up: T, right: T, down: T, left: T) = when (this) {
        Up -> up
        Right -> right
        Down -> down
        Left -> left
    }

    companion object {
        private val rightRange = -45f..45f
        private val upRange = 45f..135f
        private val downRange = -135f..-45f
        fun from(x: Number, y: Number, default: Dir = Right): Dir {
            val xf = x.toFloat()
            val yf = y.toFloat()
            if (xf == 0.0f && yf == 0.0f)
                return default
            return when (MathUtils.radiansToDegrees * atan2(yf, xf)) {
                in rightRange -> Right
                in upRange -> Up
                in downRange -> Down
                else -> Left
            }
        }
    }
}