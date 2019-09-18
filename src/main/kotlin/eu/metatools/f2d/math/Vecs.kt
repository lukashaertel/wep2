package eu.metatools.f2d.math

import java.io.Serializable

/**
 * A [Float] component accessor.
 */
interface Component {
    /**
     * Gets the [n]th component.
     */
    operator fun get(n: Int): Float

    /**
     * Sets the [n]th component.
     */
    operator fun set(n: Int, value: Float)
}

/**
 * A list of vectors sharing a backing array.
 */
class Vecs(vararg val values: Float) : Iterable<Vec>, Serializable {
    constructor(size: Int) : this(*FloatArray(size * 3))

    constructor(size: Int, init: (Int) -> Vec) : this(*FloatArray(size * 3)) {
        for (i in 0 until size) {
            val vec = init(i)
            values[i * 3 + 0] = vec.x
            values[i * 3 + 1] = vec.y
            values[i * 3 + 2] = vec.z
        }
    }

    /**
     * Constructs the vector list from a list of single vectors.
     */
    constructor(vararg vectors: Vec) : this(*FloatArray(vectors.size * 3) {
        vectors[it / 3][it % 3]
    })

    /**
     * The amount of vectors in the list.
     */
    val size = values.size / 3

    /**
     * The x-components.
     */
    val x by lazy {
        object : Component {
            override fun get(n: Int) = values[n * 3 + 0]

            override fun set(n: Int, value: Float) {
                values[n * 3 + 0] = value
            }
        }
    }

    /**
     * The y-components.
     */
    val y by lazy {
        object : Component {
            override fun get(n: Int) = values[n * 3 + 1]

            override fun set(n: Int, value: Float) {
                values[n * 3 + 1] = value
            }
        }
    }

    /**
     * The z-components.
     */
    val z by lazy {
        object : Component {
            override fun get(n: Int) = values[n * 3 + 2]

            override fun set(n: Int, value: Float) {
                values[n * 3 + 2] = value
            }
        }
    }

    /**
     * Gets a new vector of the [n]th values.
     */
    operator fun get(n: Int) = Vec(values, n * 3)

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
     * Returns the vectors as a list of [Vec].
     */
    fun toVecs() = List(size, ::get)

    /**
     * Returns the vectors as a list of [Vector3].
     */
    fun toVectors() = List(size) { get(it).toVector() }

    override fun equals(other: Any?) =
        this === other || (other as? Vecs)?.values?.contentEquals(values) ?: false

    override fun hashCode() =
        values.contentHashCode()

    override fun toString() = buildString {
        append('[')
        if (size > 0) {
            append('(')
            append(roundForPrint(values[0]))
            append(", ")
            append(roundForPrint(values[1]))
            append(", ")
            append(roundForPrint(values[2]))
            append(')')
        }
        for (i in 1 until size) {
            append(", (")
            append(roundForPrint(values[i * 3 + 0]))
            append(", ")
            append(roundForPrint(values[i * 3 + 1]))
            append(", ")
            append(roundForPrint(values[i * 3 + 2]))
            append(')')

        }
        append(']')
    }
}