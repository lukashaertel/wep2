package eu.metatools.sx.index

import eu.metatools.sx.util.Then
import eu.metatools.sx.util.then
import eu.metatools.up.dt.*

/**
 * Joins the indices [left] and [right] on a bi-directional key computation. On
 * encountering an element of [left], the element of [right] with the key [toRight]
 * applied on the key of [left] will be found and provided, and vice versa.
 * @property left The left index.
 * @property right The right index.
 * @property toRight Computes the right key from a left key.
 * @property toLeft Computes the left key from a right key.
 */
data class Join<K1 : Comparable<K1>, V1, K2 : Comparable<K2>, V2>(
    val left: Index<K1, V1>,
    val right: Index<K2, V2>,
    val toRight: (K1) -> Query<K2>,
    val toLeft: (K2) -> Query<K1>
) : Index<Then<K1, K2>, Pair<V1, V2>>() {
    override fun register(
        query: Query<Then<K1, K2>>,
        block: (Then<K1, K2>, Delta<Pair<V1, V2>>) -> Unit
    ): AutoCloseable {
        // Split query.
        val (queryLeft, queryRight) = split(query)

        // Register listener for left.
        val leftListener = left.register(queryLeft) { k1, d ->
            right.find(queryRight.merge(toRight(k1))).forEach { (k2, v2) ->
                block(k1 then k2, d.map { it to v2 })
            }
        }

        // Register listener for right.
        val rightListener = right.register(queryRight) { k2, d ->
            // For all changes on right, pair with matching left elements.
            left.find(queryLeft.merge(toLeft(k2))).forEach { (k1, v1) ->
                block(k1 then k2, d.map { v1 to it })
            }
        }

        // On close, remove both part listeners.
        return AutoCloseable {
            leftListener.close()
            rightListener.close()
        }
    }

    override fun put(key: Then<K1, K2>, value: Pair<V1, V2>): Pair<V1, V2>? {
        // Split and put respectively.
        val previousLeft = left.put(key.first, value.first)
        val previousRight = right.put(key.second, value.second)

        // Changed if any part changed.
        return (previousLeft ?: return null) to (previousRight ?: return null)
    }

    override fun remove(key: Then<K1, K2>): Pair<V1, V2>? {
        // Split and remove respectively
        val previousLeft = left.remove(key.first)
            ?: return null
        val previousRight = right.remove(key.second)
        if (previousRight == null) {
            left.put(key.first, previousLeft)
            return null
        }

        // Changed if any part changed.
        return previousLeft to previousRight
    }

    override fun find(query: Query<Then<K1, K2>>): Sequence<Pair<Then<K1, K2>, Pair<V1, V2>>> {
        // Split query.
        val (queryLeft, queryRight) = split(query)

        // Find all elements of left that match.
        return left.find(queryLeft).flatMap { (k1, v1) ->
            // Find all elements of right that match and have the proper join key.
            right.find(queryRight.merge(toRight(k1))).map { (k2, v2) ->
                (k1 then k2) to (v1 to v2)
            }
        }
    }
}

/**
 * Creates a direct natural join where key equivalence is used.
 */
infix fun <K : Comparable<K>, V1, V2> Index<K, V1>.join(other: Index<K, V2>) =
    Join(this, other, ::At, ::At)

fun main() {
    val r = IndexTree<Lx, String>()

    val x = Join(r, r, { At(it.parent) }, { Between(it / inf, it / sup) })
    x.register(Always()) { k, d ->
        println("$k: $d")
    }

    r.put(lx / 1, "Init")
    r.put(lx / 1 / 1, "A")
    r.put(lx / 1 / 2, "B")
    r.put(lx / 1 / 1 / 1, "X")
    r.put(lx / 1 / 1 / 2, "Y")
    r.put(lx / 1 / 2 / 1, "Z")

}