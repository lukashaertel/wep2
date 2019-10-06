package eu.metatools.f2d.math

import java.io.Serializable

/**
 * A list of vectors sharing a backing array.
 */
class Pts(vararg val values: Float) : Iterable<Pt>, Serializable {
    constructor(size: Int) : this(*FloatArray(size * 2))

    constructor(size: Int, init: (Int) -> Pt) : this(*FloatArray(size * 2)) {
        for (i in 0 until size) {
            val pt = init(i)
            values[i * 2 + 0] = pt.x
            values[i * 2 + 1] = pt.y
        }
    }

    /**
     * Constructs the point list from a list of single vectors.
     */
    constructor(vararg vectors: Pt) : this(*FloatArray(vectors.size * 2) {
        vectors[it / 2][it % 2]
    })

    /**
     * The amount of vectors in the list.
     */
    val size = values.size / 2

    /**
     * The x-components.
     */
    val x by lazy {
        object : Component {
            override fun get(n: Int) = values[n * 2 + 0]

            override fun set(n: Int, value: Float) {
                values[n * 2 + 0] = value
            }
        }
    }

    /**
     * The y-components.
     */
    val y by lazy {
        object : Component {
            override fun get(n: Int) = values[n * 2 + 1]

            override fun set(n: Int, value: Float) {
                values[n * 2 + 1] = value
            }
        }
    }

    /**
     * Gets a new point of the [n]th values.
     */
    operator fun get(n: Int) = Pt(values, n * 2)

    operator fun component1() = get(0)
    operator fun component2() = get(1)
    operator fun component3() = get(2)
    operator fun component4() = get(3)
    operator fun component5() = get(4)
    operator fun component6() = get(5)
    operator fun component7() = get(6)
    operator fun component8() = get(7)

    override fun iterator() =
        (0 until size).asSequence().map(::get).iterator()

    /**
     * Returns the vectors as a list of [Pt].
     */
    fun toPts() = List(size, ::get)

    /**
     * Returns the vectors as a list of [Vector2].
     */
    fun toVectors() = List(size) { get(it).toVector() }

    override fun equals(other: Any?) =
        this === other || (other as? Pts)?.values?.contentEquals(values) ?: false

    override fun hashCode() =
        values.contentHashCode()

    override fun toString() = buildString {
        append('[')
        if (size > 0) {
            append('(')
            append(roundForPrint(values[0]))
            append(", ")
            append(roundForPrint(values[1]))
            append(')')
        }
        for (i in 1 until size) {
            append(", (")
            append(roundForPrint(values[i * 2 + 0]))
            append(", ")
            append(roundForPrint(values[i * 2 + 1]))
            append(')')

        }
        append(']')
    }
}

/**
 * Reads [Pts] from vec values. Ignores the Z component.
 */
fun fromVecValues(target: FloatArray) =
    Pts(target.size / 3) {
        Pt(target[it * 3 + 0], target[it * 3 + 1])
    }

/**
 * Converts the points to vec values.
 */
fun toVecValues(pts: Pts) =
    FloatArray(pts.size * 3) {
        when (it % 3) {
            0 -> pts.x[it / 3]
            1 -> pts.y[it / 3]
            else -> 0f
        }
    }
