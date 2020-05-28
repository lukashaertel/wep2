package eu.metatools.fio.data

/**
 * Per-component mask.
 */
private const val mask = 0x3FF

/**
 * Per-component mask as a [Long].
 */
private const val maskLong = 0x3FFL

/**
 * Position of the sign in a packed component.
 */
private const val signPos = 0x200

/**
 * Flipped sign pad.
 */
private const val signPad = 0x400

private const val leftMask = maskLong.shl(50)
private const val rightMask = maskLong.shl(40)
private const val backMask = maskLong.shl(30)
private const val frontMask = maskLong.shl(20)
private const val underMask = maskLong.shl(10)
private const val overMask = maskLong

private const val notLeftMask = maskLong.shl(50).inv()
private const val notRightMask = maskLong.shl(40).inv()
private const val notBackMask = maskLong.shl(30).inv()
private const val notFrontMask = maskLong.shl(20).inv()
private const val notUnderMask = maskLong.shl(10).inv()
private const val notOverMask = maskLong.inv()


@Suppress("nothing_to_inline")
private inline fun packLeft(value: Int): Long {
    return value.and(mask).toLong().shl(50)
}

@Suppress("nothing_to_inline")
private inline fun packRight(value: Int): Long {
    return value.and(mask).toLong().shl(40)
}

@Suppress("nothing_to_inline")
private inline fun packBack(value: Int): Long {
    return value.and(mask).toLong().shl(30)
}

@Suppress("nothing_to_inline")
private inline fun packFront(value: Int): Long {
    return value.and(mask).toLong().shl(20)
}

@Suppress("nothing_to_inline")
private inline fun packUnder(value: Int): Long {
    return value.and(mask).toLong().shl(10)
}

@Suppress("nothing_to_inline")
private inline fun packOver(value: Int): Long {
    return value.and(mask).toLong()
}

@Suppress("nothing_to_inline")
private inline fun pack(left: Int, right: Int, back: Int, front: Int, under: Int, over: Int): Long {
    return packLeft(left) or packRight(right) or packBack(back) or packFront(front) or packUnder(under) or packOver(over)
}

@Suppress("nothing_to_inline")
private inline fun unpackLeft(packed: Long): Int {
    val result = (packed shr 50 and maskLong).toInt()
    return if (result and signPos != 0)
        result or -signPad
    else
        result
}

@Suppress("nothing_to_inline")
private inline fun unpackRight(packed: Long): Int {
    val result = (packed shr 40 and maskLong).toInt()
    return if (result and signPos != 0)
        result or -signPad
    else
        result
}

@Suppress("nothing_to_inline")
private inline fun unpackBack(packed: Long): Int {
    val result = (packed shr 30 and maskLong).toInt()
    return if (result and signPos != 0)
        result or -signPad
    else
        result
}

@Suppress("nothing_to_inline")
private inline fun unpackFront(packed: Long): Int {
    val result = (packed shr 20 and maskLong).toInt()
    return if (result and signPos != 0)
        result or -signPad
    else
        result
}

@Suppress("nothing_to_inline")
private inline fun unpackUnder(packed: Long): Int {
    val result = (packed shr 10 and maskLong).toInt()
    return if (result and signPos != 0)
        result or -signPad
    else
        result
}

@Suppress("nothing_to_inline")
private inline fun unpackOver(packed: Long): Int {
    val result = (packed and maskLong).toInt()
    return if (result and signPos != 0)
        result or -signPad
    else
        result
}

@Suppress("nothing_to_inline")
private inline fun packedAddLeft(packed: Long, value: Int): Long {
    return packLeft(unpackLeft(packed) + value) or packed.and(notLeftMask)
}

@Suppress("nothing_to_inline")
private inline fun packedAddRight(packed: Long, value: Int): Long {
    return packRight(unpackRight(packed) + value) or packed.and(notRightMask)
}

@Suppress("nothing_to_inline")
private inline fun packedAddBack(packed: Long, value: Int): Long {
    return packBack(unpackBack(packed) + value) or packed.and(notBackMask)
}

@Suppress("nothing_to_inline")
private inline fun packedAddFront(packed: Long, value: Int): Long {
    return packFront(unpackFront(packed) + value) or packed.and(notFrontMask)
}

@Suppress("nothing_to_inline")
private inline fun packedAddUnder(packed: Long, value: Int): Long {
    return packUnder(unpackUnder(packed) + value) or packed.and(notUnderMask)
}

@Suppress("nothing_to_inline")
private inline fun packedAddOver(packed: Long, value: Int): Long {
    return packOver(unpackOver(packed) + value) or packed.and(notOverMask)
}

/**
 * Packed six integer value. 10-bit per component.
 */
inline class Hex(val packed: Long) : Comparable<Hex> {
    companion object {
        /**
         * Minimum value per component, `-(2^9)`.
         */
        const val MinComponent = -512

        /**
         * Maximum value per component, `2^9-1`.
         */
        const val MaxComponent = 511

        /**
         * All zero [Hex].
         */
        val Zero = Hex(0, 0, 0, 0, 0, 0)

        /**
         * All one [Hex].
         */
        val One = Hex(1, 1, 1, 1, 1, 1)

        /**
         * Left unit [Hex].
         */
        val Left = Hex(1, 0, 0, 0, 0, 0)

        /**
         * Right unit [Hex].
         */
        val Right = Hex(0, 1, 0, 0, 0, 0)

        /**
         * Back unit [Hex].
         */
        val Back = Hex(0, 0, 1, 0, 0, 0)

        /**
         * Front unit [Hex].
         */
        val Front = Hex(0, 0, 0, 1, 0, 0)

        /**
         * Under unit [Hex].
         */
        val Under = Hex(0, 0, 0, 0, 1, 0)

        /**
         * Over unit [Hex].
         */
        val Over = Hex(0, 0, 0, 0, 0, 1)

        /**
         * Smallest possible [Hex] value.
         */
        val MinValue = Hex(MinComponent, MinComponent, MinComponent, MinComponent, MinComponent, MinComponent)

        /**
         * Largest possible [Hex] value.
         */
        val MaxValue = Hex(MaxComponent, MaxComponent, MaxComponent, MaxComponent, MaxComponent, MaxComponent)

        /**
         * Creates the [Hex] from the given arguments, coerces them in [MinComponent] and [MaxComponent].
         */
        fun from(let: Int, right: Int, back: Int, front: Int, under: Int, over: Int) = Hex(
            let.coerceIn(MinComponent, MaxComponent),
            right.coerceIn(MinComponent, MaxComponent),
            back.coerceIn(MinComponent, MaxComponent),
            front.coerceIn(MinComponent, MaxComponent),
            under.coerceIn(MinComponent, MaxComponent),
            over.coerceIn(MinComponent, MaxComponent)
        )

        /**
         * Constructs a single component [Hex].
         */
        fun left(value: Int) = Hex(packLeft(value))

        /**
         * Constructs a single component [Hex].
         */
        fun right(value: Int) = Hex(packRight(value))

        /**
         * Constructs a single component [Hex].
         */
        fun back(value: Int) = Hex(packBack(value))

        /**
         * Constructs a single component [Hex].
         */
        fun front(value: Int) = Hex(packFront(value))

        /**
         * Constructs a single component [Hex].
         */
        fun under(value: Int) = Hex(packUnder(value))

        /**
         * Constructs a single component [Hex].
         */
        fun over(value: Int) = Hex(packOver(value))
    }

    /**
     * Creates the [Hex] with the given component values.
     */
    constructor(left: Int, right: Int, back: Int, front: Int, under: Int, over: Int) : this(
        pack(left, right, back, front, under, over)
    )

    /**
     * The left-component.
     */
    val left get() = unpackLeft(packed)
    operator fun component0() = unpackLeft(packed)

    /**
     * The right-component.
     */
    val right get() = unpackRight(packed)
    operator fun component1() = unpackRight(packed)

    /**
     * The back-component.
     */
    val back get() = unpackBack(packed)
    operator fun component2() = unpackBack(packed)

    /**
     * The front-component.
     */
    val front get() = unpackFront(packed)
    operator fun component3() = unpackFront(packed)

    /**
     * The under-component.
     */
    val under get() = unpackUnder(packed)
    operator fun component4() = unpackUnder(packed)

    /**
     * The over-component.
     */
    val over get() = unpackOver(packed)
    operator fun component5() = unpackOver(packed)

    /**
     * True if components are all zero.
     */
    fun isEmpty() = packed == 0L

    /**
     * Adds a value to the [left] component.
     */
    fun plusLeft(value: Int) = Hex(packedAddLeft(packed, value))

    /**
     * Adds a value to the [right] component.
     */
    fun plusRight(value: Int) = Hex(packedAddRight(packed, value))

    /**
     * Adds a value to the [back] component.
     */
    fun plusBack(value: Int) = Hex(packedAddBack(packed, value))

    /**
     * Adds a value to the [front] component.
     */
    fun plusFront(value: Int) = Hex(packedAddFront(packed, value))

    /**
     * Adds a value to the [right] component.
     */
    fun plusUnder(value: Int) = Hex(packedAddUnder(packed, value))

    /**
     * Adds a value to the [back] component.
     */
    fun plusOver(value: Int) = Hex(packedAddOver(packed, value))

    /**
     * Returns the receiver.
     */
    operator fun unaryPlus() = this

    /**
     * Returns the component negative values of the receiver.
     */
    operator fun unaryMinus() = Hex(
        pack(
            -unpackLeft(packed), -unpackRight(packed), -unpackBack(packed),
            -unpackFront(packed), -unpackUnder(packed), -unpackOver(packed)
        )
    )

    operator fun plus(value: Hex) = Hex(
        pack(
            unpackLeft(packed) + unpackLeft(value.packed),
            unpackRight(packed) + unpackRight(value.packed),
            unpackBack(packed) + unpackBack(value.packed),
            unpackFront(packed) + unpackFront(value.packed),
            unpackUnder(packed) + unpackUnder(value.packed),
            unpackOver(packed) + unpackOver(value.packed)
        )
    )

    operator fun times(value: Hex) = Hex(
        pack(
            unpackLeft(packed) * unpackLeft(value.packed),
            unpackRight(packed) * unpackRight(value.packed),
            unpackBack(packed) * unpackBack(value.packed),
            unpackFront(packed) * unpackFront(value.packed),
            unpackUnder(packed) * unpackUnder(value.packed),
            unpackOver(packed) * unpackOver(value.packed)
        )
    )

    operator fun minus(value: Hex) = Hex(
        pack(
            unpackLeft(packed) - unpackLeft(value.packed),
            unpackRight(packed) - unpackRight(value.packed),
            unpackBack(packed) - unpackBack(value.packed),
            unpackFront(packed) - unpackFront(value.packed),
            unpackUnder(packed) - unpackUnder(value.packed),
            unpackOver(packed) - unpackOver(value.packed)
        )
    )

    operator fun div(value: Hex) = Hex(
        pack(
            unpackLeft(packed) / unpackLeft(value.packed),
            unpackRight(packed) / unpackRight(value.packed),
            unpackBack(packed) / unpackBack(value.packed),
            unpackFront(packed) / unpackFront(value.packed),
            unpackUnder(packed) / unpackUnder(value.packed),
            unpackOver(packed) / unpackOver(value.packed)
        )
    )

    operator fun rem(value: Hex) = Hex(
        pack(
            unpackLeft(packed).rem(unpackLeft(value.packed)),
            unpackRight(packed).rem(unpackRight(value.packed)),
            unpackBack(packed).rem(unpackBack(value.packed)),
            unpackFront(packed).rem(unpackFront(value.packed)),
            unpackUnder(packed).rem(unpackUnder(value.packed)),
            unpackOver(packed).rem(unpackOver(value.packed))
        )
    )


    fun plus(left: Int, right: Int, back: Int, front: Int, under: Int, over: Int) = Hex(
        pack(
            unpackLeft(packed) + left,
            unpackRight(packed) + right,
            unpackBack(packed) + back,
            unpackFront(packed) + front,
            unpackUnder(packed) + under,
            unpackOver(packed) + over
        )
    )

    fun times(left: Int, right: Int, back: Int, front: Int, under: Int, over: Int) = Hex(
        pack(
            unpackLeft(packed) * left,
            unpackRight(packed) * right,
            unpackBack(packed) * back,
            unpackFront(packed) * front,
            unpackUnder(packed) * under,
            unpackOver(packed) * over
        )
    )

    fun minus(left: Int, right: Int, back: Int, front: Int, under: Int, over: Int) = Hex(
        pack(
            unpackLeft(packed) - left,
            unpackRight(packed) - right,
            unpackBack(packed) - back,
            unpackFront(packed) - front,
            unpackUnder(packed) - under,
            unpackOver(packed) - over
        )
    )

    fun div(left: Int, right: Int, back: Int, front: Int, under: Int, over: Int) = Hex(
        pack(
            unpackLeft(packed) / left,
            unpackRight(packed) / right,
            unpackBack(packed) / back,
            unpackFront(packed) / front,
            unpackUnder(packed) / under,
            unpackOver(packed) / over
        )
    )

    fun rem(left: Int, right: Int, back: Int, front: Int, under: Int, over: Int) = Hex(
        pack(
            unpackLeft(packed).rem(left),
            unpackRight(packed).rem(right),
            unpackBack(packed).rem(back),
            unpackFront(packed).rem(front),
            unpackUnder(packed).rem(under),
            unpackOver(packed).rem(over)
        )
    )

    operator fun plus(value: Int) = Hex(
        pack(
            unpackLeft(packed) + value,
            unpackRight(packed) + value,
            unpackBack(packed) + value,
            unpackFront(packed) + value,
            unpackUnder(packed) + value,
            unpackOver(packed) + value
        )
    )

    operator fun times(value: Int) = Hex(
        pack(
            unpackLeft(packed) * value,
            unpackRight(packed) * value,
            unpackBack(packed) * value,
            unpackFront(packed) * value,
            unpackUnder(packed) * value,
            unpackOver(packed) * value
        )
    )

    operator fun minus(value: Int) = Hex(
        pack(
            unpackLeft(packed) - value,
            unpackRight(packed) - value,
            unpackBack(packed) - value,
            unpackFront(packed) - value,
            unpackUnder(packed) - value,
            unpackOver(packed) - value
        )
    )

    operator fun div(value: Int) = Hex(
        pack(
            unpackLeft(packed) / value,
            unpackRight(packed) / value,
            unpackBack(packed) / value,
            unpackFront(packed) / value,
            unpackUnder(packed) / value,
            unpackOver(packed) / value
        )
    )

    operator fun rem(value: Int) = Hex(
        pack(
            unpackLeft(packed).rem(value),
            unpackRight(packed).rem(value),
            unpackBack(packed).rem(value),
            unpackFront(packed).rem(value),
            unpackUnder(packed).rem(value),
            unpackOver(packed).rem(value)
        )
    )

    /**
     * Sum of the components.
     */
    fun sum() = unpackLeft(packed) + unpackRight(packed) + unpackBack(packed) +
            unpackFront(packed) + unpackUnder(packed) + unpackOver(packed)

    /**
     * Compares the values. Compares [over], then [under], then [front]. Then [back], then [right] and finally [left].
     */
    override fun compareTo(other: Hex): Int {
        val ru = over.compareTo(other.over)
        if (ru != 0) return ru
        val rt = under.compareTo(other.under)
        if (rt != 0) return rt
        val rs = front.compareTo(other.front)
        if (rs != 0) return rs
        val rr = back.compareTo(other.back)
        if (rr != 0) return rr
        val rq = right.compareTo(other.right)
        if (rq != 0) return rq
        return left.compareTo(other.left)
    }

    override fun toString() = "(left: $left, right: $right, back: $back, front: $front, under: $under, over: $over)"
}


/**
 * True if [Hex.isEmpty] is false.
 */
fun Hex.isNotEmpty() = !isEmpty()
