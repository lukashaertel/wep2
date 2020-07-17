package eu.metatools.fio.data

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.NumberUtils


/**
 * ABGR packed color.
 */
inline class Col(val packed: Int) {
    companion object {
        /**
         * A component.
         */
        val A = Col(1f, 0f, 0f, 0f)

        /**
         * R component.
         */
        val R = Col(0f, 1f, 0f, 0f)

        /**
         * G component.
         */
        val G = Col(0f, 0f, 1f, 0f)

        /**
         * B component.
         */
        val B = Col(0f, 0f, 0f, 1f)

        /**
         * Non-transparent black.
         */
        val Black = Col(1f, 0f, 0f, 0f)

        /**
         * Non-transparent white.
         */
        val White = Col(1f, 1f, 1f, 1f)


        /**
         * Transparent black, zero.
         */
        val Transparent = Col(0f, 0f, 0f, 0f)

        /**
         * Non-transparent red.
         */
        val Red = Col(1f, 1f, 0f, 0f)

        /**
         * Non-transparent green.
         */
        val Green = Col(1f, 0f, 1f, 0f)

        /**
         * Non-transparent blue.
         */
        val Blue = Col(1f, 0f, 0f, 1f)

        /**
         * Non-transparent yellow.
         */
        val Yellow = Col(1f, 1f, 1f, 0f)

        /**
         * Non-transparent magenta.
         */
        val Magenta = Col(1f, 1f, 0f, 1f)

        /**
         * Non-transparent cyan.
         */
        val Cyan = Col(1f, 0f, 1f, 1f)

        /**
         * Creates a color from hue/saturation/value.
         */
        fun hsv(h: Float, s: Float, v: Float) =
                hsv(1f, h, s, v)

        /**
         * Creates a color from hue/saturation/value.
         */
        fun hsv(a: Float, h: Float, s: Float, v: Float): Col {
            val x: Float = ((h / 60f + 6f) % 6f + 6f) % 6f
            val i = x.toInt()
            val f = x - i
            val p: Float = v * (1 - s)
            val q: Float = v * (1 - s * f)
            val t: Float = v * (1 - s * (1 - f))
            return when (i) {
                0 -> Col(a, v, t, p)
                1 -> Col(a, q, v, p)
                2 -> Col(a, p, v, t)
                3 -> Col(a, p, q, v)
                4 -> Col(a, t, p, v)
                else -> Col(a, v, p, q)
            }
        }
    }

    /**
     * Creates the color from the given values, coerces in range.
     */
    constructor(a: Float, r: Float, g: Float, b: Float) : this(
            ((255 * a).toInt().coerceIn(0, 255) shl 24) or
                    ((255 * b).toInt().coerceIn(0, 255) shl 16) or
                    ((255 * g).toInt().coerceIn(0, 255) shl 8) or
                    ((255 * r).toInt().coerceIn(0, 255))
    )

    /**
     * Creates the color from the given value.
     */
    constructor(color: Color) : this(color.toIntBits())

    /**
     * The alpha component.
     */
    val a get() = ((packed ushr 24) and 0xff) / 255f

    /**
     * The blue component.
     */
    val b get() = ((packed ushr 16) and 0xff) / 255f

    /**
     * The green component.
     */
    val g get() = ((packed ushr 8) and 0xff) / 255f

    /**
     * The red component.
     */
    val r get() = (packed and 0xff) / 255f

    /**
     * Adds and coerces the color.
     */
    operator fun plus(other: Col) =
            Col(a + other.a, r + other.r, g + other.g, b + other.b)

    /**
     * Subtracts and coerces the color.
     */
    operator fun minus(other: Col) =
            Col(a - other.a, r - other.r, g - other.g, b - other.b)

    /**
     * Multiplies and coerces the color.
     */
    operator fun times(other: Col) =
            Col(a * other.a, r * other.r, g * other.g, b * other.b)

    /**
     * Divides and coerces the color.
     */
    operator fun div(other: Col) =
            Col(a / other.a, r / other.r, g / other.g, b / other.b)

    /**
     * Sets and returns the given color to the own value.
     */
    fun toColor(color: Color) = color.also {
        Color.abgr8888ToColor(it, NumberUtils.intToFloatColor(packed))
    }

    /**
     * Gets a new color with the values assigned from the receiver.
     */
    fun toColor() = toColor(Color())

    override fun toString() = "(a: $a, r: $r, g: $g, b: $b)"
}