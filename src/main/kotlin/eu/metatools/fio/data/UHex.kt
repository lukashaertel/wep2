package eu.metatools.fio.data

/**
 * Packed six float value. 10-bit per component. Values from `-1f` to `1f`. Very low precision.
 */
inline class UHex(val actual: Hex) : Comparable<UHex> {
    companion object {
        /**
         * Minimum value per component, `-1f`.
         */
        const val MinComponent = -1f

        /**
         * Maximum value per component, `1f`.
         */
        const val MaxComponent = 1f

        /**
         * All zero [UHex].
         */
        val Zero = UHex(0f, 0f, 0f, 0f, 0f, 0f)

        /**
         * All one [UHex].
         */
        val One = UHex(1f, 1f, 1f, 1f, 1f, 1f)

        /**
         * Left unit [UHex].
         */
        val Left = UHex(1f, 0f, 0f, 0f, 0f, 0f)

        /**
         * Right unit [UHex].
         */
        val Right = UHex(0f, 1f, 0f, 0f, 0f, 0f)

        /**
         * Back unit [UHex].
         */
        val Back = UHex(0f, 0f, 1f, 0f, 0f, 0f)

        /**
         * Front unit [UHex].
         */
        val Front = UHex(0f, 0f, 0f, 1f, 0f, 0f)

        /**
         * Under unit [UHex].
         */
        val Under = UHex(0f, 0f, 0f, 0f, 1f, 0f)

        /**
         * Over unit [UHex].
         */
        val Over = UHex(0f, 0f, 0f, 0f, 0f, 1f)

        /**
         * Smallest possible [UHex] value.
         */
        val MinValue = UHex(MinComponent, MinComponent, MinComponent, MinComponent, MinComponent, MinComponent)

        /**
         * Largest possible [UHex] value.
         */
        val MaxValue = UHex(MaxComponent, MaxComponent, MaxComponent, MaxComponent, MaxComponent, MaxComponent)

        /**
         * Creates the [UHex] from the given arguments, coerces them in [MinComponent] and [MaxComponent].
         */
        fun from(left: Float, right: Float, back: Float, front: Float, under: Float, over: Float) = UHex(
            left.coerceIn(MinComponent, MaxComponent),
            right.coerceIn(MinComponent, MaxComponent),
            back.coerceIn(MinComponent, MaxComponent),
            front.coerceIn(MinComponent, MaxComponent),
            under.coerceIn(MinComponent, MaxComponent),
            over.coerceIn(MinComponent, MaxComponent)
        )

        /**
         * Constructs a single component [UHex].
         */
        fun left(value: Float) = UHex(Hex.left(value.times(Tri.MaxComponent).toInt()))

        /**
         * Constructs a single component [UHex].
         */
        fun right(value: Float) = UHex(Hex.right(value.times(Tri.MaxComponent).toInt()))

        /**
         * Constructs a single component [UHex].
         */
        fun back(value: Float) = UHex(Hex.back(value.times(Tri.MaxComponent).toInt()))

        /**
         * Constructs a single component [UHex].
         */
        fun front(value: Float) = UHex(Hex.front(value.times(Tri.MaxComponent).toInt()))

        /**
         * Constructs a single component [UHex].
         */
        fun under(value: Float) = UHex(Hex.under(value.times(Tri.MaxComponent).toInt()))

        /**
         * Constructs a single component [UHex].
         */
        fun over(value: Float) = UHex(Hex.over(value.times(Tri.MaxComponent).toInt()))
    }

    /**
     * Creates the [UHex] with the given component values.
     */
    constructor(left: Float, right: Float, back: Float, front: Float, under: Float, over: Float) : this(
        Hex(
            left.times(Hex.MaxComponent).toInt(),
            right.times(Hex.MaxComponent).toInt(),
            back.times(Hex.MaxComponent).toInt(),
            front.times(Hex.MaxComponent).toInt(),
            under.times(Hex.MaxComponent).toInt(),
            over.times(Hex.MaxComponent).toInt()
        )
    )

    /**
     * The left-component.
     */
    val left get() = actual.left.toFloat().div(Hex.MaxComponent)
    operator fun component0() = actual.left.toFloat().div(Hex.MaxComponent)

    /**
     * The right-component.
     */
    val right get() = actual.right.toFloat().div(Hex.MaxComponent)
    operator fun component1() = actual.right.toFloat().div(Hex.MaxComponent)

    /**
     * The back-component.
     */
    val back get() = actual.back.toFloat().div(Hex.MaxComponent)
    operator fun component2() = actual.back.toFloat().div(Hex.MaxComponent)

    /**
     * The front-component.
     */
    val front get() = actual.front.toFloat().div(Hex.MaxComponent)
    operator fun component3() = actual.front.toFloat().div(Hex.MaxComponent)

    /**
     * The under-component.
     */
    val under get() = actual.under.toFloat().div(Hex.MaxComponent)
    operator fun component4() = actual.under.toFloat().div(Hex.MaxComponent)

    /**
     * The over-component.
     */
    val over get() = actual.over.toFloat().div(Hex.MaxComponent)
    operator fun component5() = actual.over.toFloat().div(Hex.MaxComponent)

    /**
     * True if components are all zero.
     */
    fun isEmpty() = actual.isEmpty()

    /**
     * Adds a value to the [left] component.
     */
    fun plusLeft(value: Float) = UHex(actual.plusLeft(value.times(Hex.MaxComponent).toInt()))

    /**
     * Adds a value to the [right] component.
     */
    fun plusRight(value: Float) = UHex(actual.plusRight(value.times(Hex.MaxComponent).toInt()))

    /**
     * Adds a value to the [back] component.
     */
    fun plusBack(value: Float) = UHex(actual.plusBack(value.times(Hex.MaxComponent).toInt()))

    /**
     * Adds a value to the [left] component.
     */
    fun plusFront(value: Float) = UHex(actual.plusFront(value.times(Hex.MaxComponent).toInt()))

    /**
     * Adds a value to the [right] component.
     */
    fun plusUnder(value: Float) = UHex(actual.plusUnder(value.times(Hex.MaxComponent).toInt()))

    /**
     * Adds a value to the [back] component.
     */
    fun plusOver(value: Float) = UHex(actual.plusOver(value.times(Hex.MaxComponent).toInt()))

    /**
     * Returns the receiver.
     */
    operator fun unaryPlus() = this

    /**
     * Returns the component negative values of the receiver.
     */
    operator fun unaryMinus() = UHex(actual.unaryMinus())

    operator fun plus(value: UHex) = UHex(actual + value.actual)

    operator fun times(value: UHex) = UHex(
        Hex(
            actual.left * value.actual.left / Hex.MaxComponent,
            actual.right * value.actual.right / Hex.MaxComponent,
            actual.back * value.actual.back / Hex.MaxComponent,
            actual.front * value.actual.front / Hex.MaxComponent,
            actual.under * value.actual.under / Hex.MaxComponent,
            actual.over * value.actual.over / Hex.MaxComponent
        )
    )

    operator fun minus(value: UHex) = UHex(actual - value.actual)

    operator fun div(value: UHex) = UHex(
        Hex(
            actual.left * Hex.MaxComponent / value.actual.left,
            actual.right * Hex.MaxComponent / value.actual.right,
            actual.back * Hex.MaxComponent / value.actual.back,
            actual.front * Hex.MaxComponent / value.actual.front,
            actual.under * Hex.MaxComponent / value.actual.under,
            actual.over * Hex.MaxComponent / value.actual.over
        )
    )

    fun plus(left: Float, right: Float, back: Float, front: Float, under: Float, over: Float) = UHex(
        actual.plus(
            left.times(Hex.MaxComponent).toInt(),
            right.times(Hex.MaxComponent).toInt(),
            back.times(Hex.MaxComponent).toInt(),
            front.times(Hex.MaxComponent).toInt(),
            under.times(Hex.MaxComponent).toInt(),
            over.times(Hex.MaxComponent).toInt()
        )
    )

    fun times(left: Float, right: Float, back: Float, front: Float, under: Float, over: Float) = UHex(
        Hex(
            actual.left * left.times(Hex.MaxComponent).toInt() / Hex.MaxComponent,
            actual.right * right.times(Hex.MaxComponent).toInt() / Hex.MaxComponent,
            actual.back * back.times(Hex.MaxComponent).toInt() / Hex.MaxComponent,
            actual.front * front.times(Hex.MaxComponent).toInt() / Hex.MaxComponent,
            actual.under * under.times(Hex.MaxComponent).toInt() / Hex.MaxComponent,
            actual.over * over.times(Hex.MaxComponent).toInt() / Hex.MaxComponent
        )
    )

    fun minus(left: Float, right: Float, back: Float, front: Float, under: Float, over: Float) = UHex(
        actual.minus(
            left.times(Hex.MaxComponent).toInt(),
            right.times(Hex.MaxComponent).toInt(),
            back.times(Hex.MaxComponent).toInt(),
            front.times(Hex.MaxComponent).toInt(),
            under.times(Hex.MaxComponent).toInt(),
            over.times(Hex.MaxComponent).toInt()
        )
    )

    fun div(left: Float, right: Float, back: Float, front: Float, under: Float, over: Float) = UHex(
        Hex(
            actual.left * Hex.MaxComponent / left.times(Hex.MaxComponent).toInt(),
            actual.right * Hex.MaxComponent / right.times(Hex.MaxComponent).toInt(),
            actual.back * Hex.MaxComponent / back.times(Hex.MaxComponent).toInt(),
            actual.front * Hex.MaxComponent / front.times(Hex.MaxComponent).toInt(),
            actual.under * Hex.MaxComponent / under.times(Hex.MaxComponent).toInt(),
            actual.over * Hex.MaxComponent / over.times(Hex.MaxComponent).toInt()
        )
    )

    operator fun plus(value: Float) =
        UHex(actual.plus(value.times(Hex.MaxComponent).toInt()))

    operator fun times(value: Float): UHex {
        val converted = value.times(Hex.MaxComponent).toInt()
        return UHex(
            Hex(
                actual.left * converted / Hex.MaxComponent,
                actual.right * converted / Hex.MaxComponent,
                actual.back * converted / Hex.MaxComponent,
                actual.front * converted / Hex.MaxComponent,
                actual.under * converted / Hex.MaxComponent,
                actual.over * converted / Hex.MaxComponent
            )
        )
    }

    operator fun minus(value: Float) =
        UHex(actual.minus(value.times(Hex.MaxComponent).toInt()))

    operator fun div(value: Float): UHex {
        val converted = value.times(Hex.MaxComponent).toInt()
        return UHex(
            Hex(
                actual.left * Hex.MaxComponent / converted,
                actual.right * Hex.MaxComponent / converted,
                actual.back * Hex.MaxComponent / converted,
                actual.front * Hex.MaxComponent / converted,
                actual.under * Hex.MaxComponent / converted,
                actual.over * Hex.MaxComponent / converted
            )
        )
    }

    /**
     * Compares the values. Compares [over], then [under], then [front]. Then [back], then [right] and finally [left].
     */
    override fun compareTo(other: UHex) =
        actual.compareTo(other.actual)

    override fun toString() = "(left: $left, right: $right, back: $back, front: $front, under: $under, over: $over)"
}


/**
 * True if [UHex.isEmpty] is false.
 */
fun UHex.isNotEmpty() = !isEmpty()
