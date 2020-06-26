package eu.metatools.fio.data

/**
 * Packed three float value. 21-bit per component. Values from `-1f` to `1f`.
 */
inline class UTri(val actual: Tri) : Comparable<UTri> {
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
         * All zero [UTri].
         */
        val Zero = UTri(0f, 0f, 0f)

        /**
         * All one [UTri].
         */
        val One = UTri(1f, 1f, 1f)

        /**
         * X unit [UTri].
         */
        val X = UTri(1f, 0f, 0f)

        /**
         * Y unit [UTri].
         */
        val Y = UTri(0f, 1f, 0f)

        /**
         * Z unit [UTri].
         */
        val Z = UTri(0f, 0f, 1f)

        /**
         * Smallest possible [UTri] value.
         */
        val MinValue = UTri(MinComponent, MinComponent, MinComponent)

        /**
         * Largest possible [UTri] value.
         */
        val MaxValue = UTri(MaxComponent, MaxComponent, MaxComponent)

        /**
         * Creates the [UTri] from the given arguments, coerces them in [MinComponent] and [MaxComponent].
         */
        fun from(x: Float, y: Float, z: Float) = UTri(
            x.coerceIn(MinComponent, MaxComponent),
            y.coerceIn(MinComponent, MaxComponent),
            z.coerceIn(MinComponent, MaxComponent)
        )

        /**
         * Constructs a single component [UTri].
         */
        fun x(value: Float) = UTri(Tri.x(value.times(Tri.MaxComponent).toInt()))

        /**
         * Constructs a single component [UTri].
         */
        fun y(value: Float) = UTri(Tri.y(value.times(Tri.MaxComponent).toInt()))

        /**
         * Constructs a single component [UTri].
         */
        fun z(value: Float) =  UTri(Tri.z(value.times(Tri.MaxComponent).toInt()))
    }

    /**
     * Creates the [UTri] with the given component values.
     */
    constructor(x: Float, y: Float, z: Float) : this(
        Tri(
            x.times(Tri.MaxComponent).toInt(),
            y.times(Tri.MaxComponent).toInt(),
            z.times(Tri.MaxComponent).toInt()
        )
    )

    /**
     * Returns a new [UTri] with only [x] and [y] copied.
     */
    val xy get() = UTri(actual.xy)

    /**
     * Returns a new [UTri] with only [x] and [z] copied.
     */
    val xz get() = UTri(actual.xz)

    /**
     * Returns a new [UTri] with only [y] and [y] copied.
     */
    val yz get() = UTri(actual.yz)

    /**
     * The x-component.
     */
    val x get() = actual.x.toFloat().div(Tri.MaxComponent)
    operator fun component0() = actual.x.toFloat().div(Tri.MaxComponent)

    /**
     * The y-component.
     */
    val y get() = actual.y.toFloat().div(Tri.MaxComponent)
    operator fun component1() = actual.y.toFloat().div(Tri.MaxComponent)

    /**
     * The z-component.
     */
    val z get() = actual.z.toFloat().div(Tri.MaxComponent)
    operator fun component2() = actual.z.toFloat().div(Tri.MaxComponent)

    /**
     * True if components are all zero.
     */
    fun isEmpty() = actual.isEmpty()

    /**
     * Adds a value to the [x] component.
     */
    fun plusX(value: Float) = UTri(actual.plusX(value.times(Tri.MaxComponent).toInt()))

    /**
     * Adds a value to the [y] component.
     */
    fun plusY(value: Float) = UTri(actual.plusY(value.times(Tri.MaxComponent).toInt()))

    /**
     * Adds a value to the [z] component.
     */
    fun plusZ(value: Float) = UTri(actual.plusZ(value.times(Tri.MaxComponent).toInt()))

    /**
     * Returns the receiver.
     */
    operator fun unaryPlus() = this

    /**
     * Returns the component negative values of the receiver.
     */
    operator fun unaryMinus() = UTri(actual.unaryMinus())

    operator fun plus(value: UTri) = UTri(actual + value.actual)

    operator fun times(value: UTri) = UTri(
        Tri(
            actual.x * value.actual.x / Tri.MaxComponent,
            actual.y * value.actual.y / Tri.MaxComponent,
            actual.z * value.actual.z / Tri.MaxComponent
        )
    )

    operator fun minus(value: UTri) = UTri(actual - value.actual)

    operator fun div(value: UTri) = UTri(
        Tri(
            actual.x * Tri.MaxComponent / value.actual.x,
            actual.y * Tri.MaxComponent / value.actual.y,
            actual.z * Tri.MaxComponent / value.actual.z
        )
    )

    fun plus(x: Float, y: Float, z: Float) = UTri(
        actual.plus(
            x.times(Tri.MaxComponent).toInt(),
            y.times(Tri.MaxComponent).toInt(),
            z.times(Tri.MaxComponent).toInt()
        )
    )

    fun times(x: Float, y: Float, z: Float) = UTri(
        Tri(
            actual.x * x.times(Tri.MaxComponent).toInt() / Tri.MaxComponent,
            actual.y * y.times(Tri.MaxComponent).toInt() / Tri.MaxComponent,
            actual.z * z.times(Tri.MaxComponent).toInt() / Tri.MaxComponent
        )
    )

    fun minus(x: Float, y: Float, z: Float) = UTri(
        actual.minus(
            x.times(Tri.MaxComponent).toInt(),
            y.times(Tri.MaxComponent).toInt(),
            z.times(Tri.MaxComponent).toInt()
        )
    )

    fun div(x: Float, y: Float, z: Float) = UTri(
        Tri(
            actual.x * Tri.MaxComponent / x.times(Tri.MaxComponent).toInt(),
            actual.y * Tri.MaxComponent / y.times(Tri.MaxComponent).toInt(),
            actual.z * Tri.MaxComponent / z.times(Tri.MaxComponent).toInt()
        )
    )

    operator fun plus(value: Float) =
        UTri(actual.plus(value.times(Tri.MaxComponent).toInt()))

    operator fun times(value: Float): UTri {
        val converted = value.times(Tri.MaxComponent).toInt()
        return UTri(
            Tri(
                actual.x * converted / Tri.MaxComponent,
                actual.y * converted / Tri.MaxComponent,
                actual.z * converted / Tri.MaxComponent
            )
        )
    }


    operator fun minus(value: Float) =
        UTri(actual.minus(value.times(Tri.MaxComponent).toInt()))


    operator fun div(value: Float): UTri {
        val converted = value.times(Tri.MaxComponent).toInt()
        return UTri(
            Tri(
                actual.x * Tri.MaxComponent / converted,
                actual.y * Tri.MaxComponent / converted,
                actual.z * Tri.MaxComponent / converted
            )
        )
    }


    /**
     * Dot product.
     */
    infix fun dot(other: UTri) =
        x * other.x + y * other.y + z * other.z

    /**
     * Compares the values. Compares [z], then [y], then [x].
     */
    override fun compareTo(other: UTri) =
        actual.compareTo(other.actual)

    override fun toString() = "($x, $y, $z)"
}
