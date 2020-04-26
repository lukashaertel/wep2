package eu.metatools.sx.data

import eu.metatools.fio.data.Tri

/**
 * Joins two volumes on coordinates. The [combine] function will be invoked with at least one non-null argument.
 */
inline fun <T : Any, U : Any, R : Any> Volume<T>.join(
    other: Volume<U>,
    crossinline separateLeft: (R) -> T?,
    crossinline separateRight: (R) -> U?,
    crossinline combine: (T?, U?) -> R
) = object : Volume<R> {
    override fun set(x: Int, y: Int, z: Int, value: R): R? {
        val left = separateLeft(value)?.let { this@join.set(x, y, z, it) }
        val right = separateRight(value)?.let { other.set(x, y, z, it) }
        return if (left != null || right != null)
            combine(left, right)
        else
            null
    }

    override fun remove(x: Int, y: Int, z: Int): R? {
        val left = this@join.remove(x, y, z)
        val right = other.remove(x, y, z)
        return if (left != null || right != null)
            combine(left, right)
        else
            null
    }

    override fun contains(x: Int, y: Int, z: Int): Boolean {
        return this@join.contains(x, y, z) || other.contains(x, y, z)
    }

    override fun get(x: Int, y: Int, z: Int): R? {
        val left = this@join[x, y, z]
        val right = other[x, y, z]
        return if (left != null || right != null)
            combine(left, right)
        else
            null
    }

    override fun get(x: IntRange, y: IntRange, z: IntRange): Sequence<Pair<Tri, R>> {
        // Join from left items.
        val first = this@join[x, y, z].map { (at, left) ->
            val right = other[at.x, at.y, at.z]
            at to combine(left, right)
        }

        // Join from right items where not already visited by left.
        val second = other[x, y, z].filter { (at, _) ->
            !this@join.contains(at.x, at.y, at.z)
        }.map { (at, right) ->
            val left = this@join[at.x, at.y, at.z]
            at to combine(left, right)
        }

        // Return both.
        return first + second
    }

    override fun getAll(): Sequence<Pair<Tri, R>> {
        // Join from left items.
        val first = this@join.getAll().map { (at, left) ->
            val right = other[at.x, at.y, at.z]
            at to combine(left, right)
        }

        // Join from right items where not already visited by left.
        val second = other.getAll().filter { (at, _) ->
            !this@join.contains(at.x, at.y, at.z)
        }.map { (at, right) ->
            val left = this@join[at.x, at.y, at.z]
            at to combine(left, right)
        }

        // Return both.
        return first + second
    }

    override fun findCenter(): Tri {
        val sizeLeft = this@join.size
        val sizeRight = other.size
        val total = sizeLeft + sizeRight

        val centerLeft = this@join.findCenter()
        val centerRight = other.findCenter()

        return Tri(
            (centerLeft.x * sizeLeft + centerRight.x * sizeRight) / total,
            (centerLeft.y * sizeLeft + centerRight.y * sizeRight) / total,
            (centerLeft.z * sizeLeft + centerRight.z * sizeRight) / total
        )
    }

    override val size: Int
        get() = maxOf(this@join.size, other.size)

    override fun toString() =
        if (size > 10)
            "Join Volume {" + getAll().take(10).joinToString() + ", ... }"
        else
            "Join Volume {" + getAll().joinToString() + "}"
}

/**
 * Joins two volumes to a pair volume. Pairs will have at least one entry assigned.
 */
infix fun <T : Any, U : Any> Volume<T>.join(other: Volume<U>) =
    join(other, { it.first }, { it.second }) { l, r -> l to r }
