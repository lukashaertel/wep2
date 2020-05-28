package eu.metatools.fio.data

import eu.metatools.sx.ents.Dirs

/**
 * Per-component mask.
 */
private const val mask = 0x1FFFFF

/**
 * Per-component mask as a [Long].
 */
private const val maskLong = 0x1FFFFFL

/**
 * Position of the sign in a packed component.
 */
private const val signPos = 0x100000

/**
 * Flipped sign pad.
 */
private const val signPad = 0x200000

/**
 * Component mask for the x component.
 */
private const val xMask = maskLong.shl(42)

/**
 * Component mask for the y component.
 */
private const val yMask = maskLong.shl(21)

/**
 * Component mask for the z component.
 */
private const val zMask = maskLong

/**
 * Component mask for the x and y components.
 */
private const val xyMask = xMask or yMask

/**
 * Component mask for the x and z components.
 */
private const val xzMask = xMask or zMask

/**
 * Component mask for the y and z components.
 */
private const val yzMask = yMask or zMask

@Suppress("nothing_to_inline")
private inline fun packX(value: Int): Long {
    return value.and(mask).toLong().shl(42)
}

@Suppress("nothing_to_inline")
private inline fun packY(value: Int): Long {
    return value.and(mask).toLong().shl(21)
}

@Suppress("nothing_to_inline")
private inline fun packZ(value: Int): Long {
    return value.and(mask).toLong()
}

@Suppress("nothing_to_inline")
private inline fun pack(x: Int, y: Int, z: Int): Long {
    return packX(x) or packY(y) or packZ(z)
}

@Suppress("nothing_to_inline")
private inline fun unpackX(packed: Long): Int {
    val result = (packed shr 42 and maskLong).toInt()
    return if (result and signPos != 0)
        result or -signPad
    else
        result
}

@Suppress("nothing_to_inline")
private inline fun unpackY(packed: Long): Int {
    val result = (packed shr 21 and maskLong).toInt()
    return if (result and signPos != 0)
        result or -signPad
    else
        result
}

@Suppress("nothing_to_inline")
private inline fun unpackZ(packed: Long): Int {
    val result = (packed and maskLong).toInt()
    return if (result and signPos != 0)
        result or -signPad
    else
        result
}

@Suppress("nothing_to_inline")
private inline fun packedAddX(packed: Long, value: Int): Long {
    return packX(unpackX(packed) + value) or packed.and(yzMask)
}

@Suppress("nothing_to_inline")
private inline fun packedAddY(packed: Long, value: Int): Long {
    return packY(unpackY(packed) + value) or packed.and(xzMask)
}

@Suppress("nothing_to_inline")
private inline fun packedAddZ(packed: Long, value: Int): Long {
    return packZ(unpackZ(packed) + value) or packed.and(xyMask)
}

/**
 * Packed three integer value. 21-bit per component.
 */
inline class Tri(val packed: Long) : Comparable<Tri> {
    companion object {
        /**
         * Minimum value per component, `-(2^20)`.
         */
        const val MinComponent = -1048576

        /**
         * Maximum value per component, `2^20-1`.
         */
        const val MaxComponent = 1048575

        /**
         * All zero [Tri].
         */
        val Zero = Tri(0, 0, 0)

        /**
         * All one [Tri].
         */
        val One = Tri(1, 1, 1)

        /**
         * X unit [Tri].
         */
        val X = Tri(1, 0, 0)

        /**
         * Y unit [Tri].
         */
        val Y = Tri(0, 1, 0)

        /**
         * Z unit [Tri].
         */
        val Z = Tri(0, 0, 1)

        /**
         * Smallest possible [Tri] value.
         */
        val MinValue = Tri(MinComponent, MinComponent, MinComponent)

        /**
         * Largest possible [Tri] value.
         */
        val MaxValue = Tri(MaxComponent, MaxComponent, MaxComponent)

        /**
         * Creates the [Tri] from the given arguments, coerces them in [MinComponent] and [MaxComponent].
         */
        fun from(x: Int, y: Int, z: Int) = Tri(
            x.coerceIn(MinComponent, MaxComponent),
            y.coerceIn(MinComponent, MaxComponent),
            z.coerceIn(MinComponent, MaxComponent)
        )

        /**
         * Constructs a single component [Tri].
         */
        fun x(value: Int) = Tri(packX(value))

        /**
         * Constructs a single component [Tri].
         */
        fun y(value: Int) = Tri(packY(value))

        /**
         * Constructs a single component [Tri].
         */
        fun z(value: Int) = Tri(packZ(value))
    }

    /**
     * Creates the [Tri] with the given component values.
     */
    constructor(x: Int, y: Int, z: Int) : this(pack(x, y, z))

    /**
     * Returns a new [Tri] with only [x] and [y] copied.
     */
    val xy get() = Tri(packed and xyMask)

    /**
     * Returns a new [Tri] with only [x] and [z] copied.
     */
    val xz get() = Tri(packed and xzMask)

    /**
     * Returns a new [Tri] with only [y] and [y] copied.
     */
    val yz get() = Tri(packed and yzMask)

    /**
     * The x-component.
     */
    val x get() = unpackX(packed)
    operator fun component0() = unpackX(packed)

    /**
     * The y-component.
     */
    val y get() = unpackY(packed)
    operator fun component1() = unpackY(packed)

    /**
     * The z-component.
     */
    val z get() = unpackZ(packed)
    operator fun component2() = unpackZ(packed)

    /**
     * True if components are all zero.
     */
    fun isEmpty() = packed == 0L

    /**
     * Adds a value to the [x] component.
     */
    fun plusX(value: Int) = Tri(packedAddX(packed, value))

    /**
     * Adds a value to the [y] component.
     */
    fun plusY(value: Int) = Tri(packedAddY(packed, value))

    /**
     * Adds a value to the [z] component.
     */
    fun plusZ(value: Int) = Tri(packedAddZ(packed, value))

    /**
     * Returns the receiver.
     */
    operator fun unaryPlus() = this

    /**
     * Returns the component negative values of the receiver.
     */
    operator fun unaryMinus() = Tri(pack(-unpackX(packed), -unpackY(packed), -unpackZ(packed)))

    operator fun plus(value: Tri) = Tri(
        pack(
            unpackX(packed) + unpackX(value.packed),
            unpackY(packed) + unpackY(value.packed),
            unpackZ(packed) + unpackZ(value.packed)
        )
    )

    operator fun times(value: Tri) = Tri(
        pack(
            unpackX(packed) * unpackX(value.packed),
            unpackY(packed) * unpackY(value.packed),
            unpackZ(packed) * unpackZ(value.packed)
        )
    )

    operator fun minus(value: Tri) = Tri(
        pack(
            unpackX(packed) - unpackX(value.packed),
            unpackY(packed) - unpackY(value.packed),
            unpackZ(packed) - unpackZ(value.packed)
        )
    )

    operator fun div(value: Tri) = Tri(
        pack(
            unpackX(packed) / unpackX(value.packed),
            unpackY(packed) / unpackY(value.packed),
            unpackZ(packed) / unpackZ(value.packed)
        )
    )

    operator fun rem(value: Tri) = Tri(
        pack(
            unpackX(packed).rem(unpackX(value.packed)),
            unpackY(packed).rem(unpackY(value.packed)),
            unpackZ(packed).rem(unpackZ(value.packed))
        )
    )


    fun plus(x: Int, y: Int, z: Int) = Tri(
        pack(
            unpackX(packed) + x,
            unpackY(packed) + y,
            unpackZ(packed) + z
        )
    )

    fun times(x: Int, y: Int, z: Int) = Tri(
        pack(
            unpackX(packed) * x,
            unpackY(packed) * y,
            unpackZ(packed) * z
        )
    )

    fun minus(x: Int, y: Int, z: Int) = Tri(
        pack(
            unpackX(packed) - x,
            unpackY(packed) - y,
            unpackZ(packed) - z
        )
    )

    fun div(x: Int, y: Int, z: Int) = Tri(
        pack(
            unpackX(packed) / x,
            unpackY(packed) / y,
            unpackZ(packed) / z
        )
    )

    fun rem(x: Int, y: Int, z: Int) = Tri(
        pack(
            unpackX(packed).rem(x),
            unpackY(packed).rem(y),
            unpackZ(packed).rem(z)
        )
    )

    operator fun plus(value: Int) = Tri(
        pack(
            unpackX(packed) + value,
            unpackY(packed) + value,
            unpackZ(packed) + value
        )
    )

    operator fun times(value: Int) = Tri(
        pack(
            unpackX(packed) * value,
            unpackY(packed) * value,
            unpackZ(packed) * value
        )
    )

    operator fun minus(value: Int) = Tri(
        pack(
            unpackX(packed) - value,
            unpackY(packed) - value,
            unpackZ(packed) - value
        )
    )

    operator fun div(value: Int) = Tri(
        pack(
            unpackX(packed) / value,
            unpackY(packed) / value,
            unpackZ(packed) / value
        )
    )

    operator fun rem(value: Int) = Tri(
        pack(
            unpackX(packed).rem(value),
            unpackY(packed).rem(value),
            unpackZ(packed).rem(value)
        )
    )

    /**
     * Sum of the components.
     */
    fun sum() = unpackX(packed) + unpackY(packed) + unpackZ(packed)

    /**
     * Dot product.
     */
    infix fun dot(other: Tri) =
        unpackX(packed) * unpackX(other.packed) +
                unpackY(packed) * unpackY(other.packed) +
                unpackZ(packed) * unpackZ(other.packed)

    /**
     * Compares the values. Compares [z], then [y], then [x].
     */
    override fun compareTo(other: Tri): Int {
        val rz = z.compareTo(other.z)
        if (rz != 0) return rz
        val ry = y.compareTo(other.y)
        if (ry != 0) return ry
        return x.compareTo(other.x)
    }

    override fun toString() = "($x, $y, $z)"
}

/**
 * True if [Tri.isEmpty] is false.
 */
fun Tri.isNotEmpty() = !isEmpty()

/**
 * Shorthand ofr [Tri.plusX] with `-1`.
 */
fun Tri.left() = plusX(-1)

/**
 * Shorthand ofr [Tri.plusX] with `1`.
 */
fun Tri.right() = plusX(1)

/**
 * Shorthand ofr [Tri.plusY] with `-1`.
 */
fun Tri.back() = plusY(-1)

/**
 * Shorthand ofr [Tri.plusY] with `1`.
 */
fun Tri.front() = plusY(1)

/**
 * Shorthand ofr [Tri.plusZ] with `-1`.
 */
fun Tri.under() = plusZ(-1)

/**
 * Shorthand ofr [Tri.plusZ] with `1`.
 */
fun Tri.over() = plusZ(1)

/**
 * Returns the cross around the receiver, including the receiver.
 */
fun Tri.cross() =
    sequenceOf(under(), back(), left(), this, right(), front(), over())

/**
 * Returns the directions of [Tri.cross].
 */
fun Tri.crossDirs() =
    sequenceOf(Dirs.under, Dirs.back, Dirs.left, Dirs.none, Dirs.right, Dirs.front, Dirs.over)

/**
 * Returns the cross around the receiver.
 */
fun Tri.crossPure() =
    sequenceOf(under(), back(), left(), right(), front(), over())

/**
 * Returns the directions of [Tri.crossPure].
 */
fun Tri.crossPureDirs() =
    sequenceOf(Dirs.under, Dirs.back, Dirs.left, Dirs.right, Dirs.front, Dirs.over)

/**
 * Returns the `3x3x3` cube around the receive, including the receiver.
 */
fun Tri.cube(): Sequence<Tri> {
    // Unpack components.
    val x = x
    val y = y
    val z = z

    val x0 = packX(x.dec())
    val x1 = packX(x)
    val x2 = packX(x.inc())

    val y0 = packY(y.dec())
    val y1 = packY(y)
    val y2 = packY(y.inc())

    val z0 = packZ(z.dec())
    val z1 = packZ(z)
    val z2 = packZ(z.inc())

    return sequenceOf(
        Tri(x0 or y0 or z0), Tri(x1 or y0 or z0), Tri(x2 or y0 or z0),
        Tri(x0 or y1 or z0), Tri(x1 or y1 or z0), Tri(x2 or y1 or z0),
        Tri(x0 or y2 or z0), Tri(x1 or y2 or z0), Tri(x2 or y2 or z0),
        Tri(x0 or y0 or z1), Tri(x1 or y0 or z1), Tri(x2 or y0 or z1),
        Tri(x0 or y1 or z1), Tri(x1 or y1 or z1), Tri(x2 or y1 or z1),
        Tri(x0 or y2 or z1), Tri(x1 or y2 or z1), Tri(x2 or y2 or z1),
        Tri(x0 or y0 or z2), Tri(x1 or y0 or z2), Tri(x2 or y0 or z2),
        Tri(x0 or y1 or z2), Tri(x1 or y1 or z2), Tri(x2 or y1 or z2),
        Tri(x0 or y2 or z2), Tri(x1 or y2 or z2), Tri(x2 or y2 or z2)
    )
}

/**
 * Returns the `3x3x3` cube around the receive
 */
fun Tri.cubePure(): Sequence<Tri> {
    // Unpack components.
    val x = x
    val y = y
    val z = z

    // Repack displaced.
    val x0 = packX(x.dec())
    val x1 = packX(x)
    val x2 = packX(x.inc())

    val y0 = packY(y.dec())
    val y1 = packY(y)
    val y2 = packY(y.inc())

    val z0 = packZ(z.dec())
    val z1 = packZ(z)
    val z2 = packZ(z.inc())

    return sequenceOf(
        Tri(x0 or y0 or z0), Tri(x1 or y0 or z0), Tri(x2 or y0 or z0),
        Tri(x0 or y1 or z0), Tri(x1 or y1 or z0), Tri(x2 or y1 or z0),
        Tri(x0 or y2 or z0), Tri(x1 or y2 or z0), Tri(x2 or y2 or z0),
        Tri(x0 or y0 or z1), Tri(x1 or y0 or z1), Tri(x2 or y0 or z1),
        Tri(x0 or y1 or z1), Tri(x2 or y1 or z1),
        Tri(x0 or y2 or z1), Tri(x1 or y2 or z1), Tri(x2 or y2 or z1),
        Tri(x0 or y0 or z2), Tri(x1 or y0 or z2), Tri(x2 or y0 or z2),
        Tri(x0 or y1 or z2), Tri(x1 or y1 or z2), Tri(x2 or y1 or z2),
        Tri(x0 or y2 or z2), Tri(x1 or y2 or z2), Tri(x2 or y2 or z2)
    )
}

/**
 * Returns the `3x3` cube around the receive with fixed [Tri.x], including the receiver.
 */
fun Tri.planeX(): Sequence<Tri> {
    // Unpack components.
    val xf = packX(x)
    val y = y
    val z = z

    // Repack displaced.
    val y0 = packY(y.dec())
    val y1 = packY(y)
    val y2 = packY(y.inc())

    val z0 = packZ(z.dec())
    val z1 = packZ(z)
    val z2 = packZ(z.inc())

    return sequenceOf(
        Tri(xf or y0 or z0),
        Tri(xf or y1 or z0),
        Tri(xf or y2 or z0),
        Tri(xf or y0 or z1),
        Tri(xf or y1 or z1),
        Tri(xf or y2 or z1),
        Tri(xf or y0 or z2),
        Tri(xf or y1 or z2),
        Tri(xf or y2 or z2)
    )
}

/**
 * Returns the `3x3` cube around the receive with fixed [Tri.x].
 */
fun Tri.planeXPure(): Sequence<Tri> {
    // Unpack components.
    val xf = packX(x)
    val y = y
    val z = z

    // Repack displaced.
    val y0 = packY(y.dec())
    val y1 = packY(y)
    val y2 = packY(y.inc())

    val z0 = packZ(z.dec())
    val z1 = packZ(z)
    val z2 = packZ(z.inc())

    return sequenceOf(
        Tri(xf or y0 or z0),
        Tri(xf or y1 or z0),
        Tri(xf or y2 or z0),
        Tri(xf or y0 or z1),
        Tri(xf or y2 or z1),
        Tri(xf or y0 or z2),
        Tri(xf or y1 or z2),
        Tri(xf or y2 or z2)
    )
}

/**
 * Returns the `3x3` cube around the receive with fixed [Tri.y], including the receiver.
 */
fun Tri.planeY(): Sequence<Tri> {
    // Unpack components.
    val x = x
    val yf = packY(y)
    val z = z

    // Repack displaced.
    val x0 = packX(x.dec())
    val x1 = packX(x)
    val x2 = packX(x.inc())

    val z0 = packZ(z.dec())
    val z1 = packZ(z)
    val z2 = packZ(z.inc())

    return sequenceOf(
        Tri(x0 or yf or z0), Tri(x1 or yf or z0), Tri(x2 or yf or z0),
        Tri(x0 or yf or z1), Tri(x1 or yf or z1), Tri(x2 or yf or z1),
        Tri(x0 or yf or z2), Tri(x1 or yf or z2), Tri(x2 or yf or z2)
    )
}

/**
 * Returns the `3x3` cube around the receive with fixed [Tri.y].
 */
fun Tri.planeYPure(): Sequence<Tri> {
    // Unpack components.
    val x = x
    val yf = packY(y)
    val z = z

    // Repack displaced.
    val x0 = packX(x.dec())
    val x1 = packX(x)
    val x2 = packX(x.inc())

    val z0 = packZ(z.dec())
    val z1 = packZ(z)
    val z2 = packZ(z.inc())

    return sequenceOf(
        Tri(x0 or yf or z0), Tri(x1 or yf or z0), Tri(x2 or yf or z0),
        Tri(x0 or yf or z1), Tri(x2 or yf or z1),
        Tri(x0 or yf or z2), Tri(x1 or yf or z2), Tri(x2 or yf or z2)
    )
}

/**
 * Returns the `3x3` cube around the receive with fixed [Tri.z], including the receiver.
 */
fun Tri.planeZ(): Sequence<Tri> {
    // Unpack components.
    val x = x
    val y = y
    val zf = packZ(z)

    // Repack displaced.
    val x0 = packX(x.dec())
    val x1 = packX(x)
    val x2 = packX(x.inc())

    val y0 = packY(y.dec())
    val y1 = packY(y)
    val y2 = packY(y.inc())


    return sequenceOf(
        Tri(x0 or y0 or zf), Tri(x1 or y0 or zf), Tri(x2 or y0 or zf),
        Tri(x0 or y1 or zf), Tri(x1 or y1 or zf), Tri(x2 or y1 or zf),
        Tri(x0 or y2 or zf), Tri(x1 or y2 or zf), Tri(x2 or y2 or zf)
    )
}

/**
 * Returns the `3x3` cube around the receive with fixed [Tri.z].
 */
fun Tri.planeZPure(): Sequence<Tri> {
    // Unpack components.
    val x = x
    val y = y
    val zf = packZ(z)

    // Repack displaced.
    val x0 = packX(x.dec())
    val x1 = packX(x)
    val x2 = packX(x.inc())

    val y0 = packY(y.dec())
    val y1 = packY(y)
    val y2 = packY(y.inc())


    return sequenceOf(
        Tri(x0 or y0 or zf), Tri(x1 or y0 or zf), Tri(x2 or y0 or zf),
        Tri(x0 or y1 or zf), Tri(x2 or y1 or zf),
        Tri(x0 or y2 or zf), Tri(x1 or y2 or zf), Tri(x2 or y2 or zf)
    )
}