package eu.metatools.sx.index

import eu.metatools.sx.util.Then
import eu.metatools.sx.util.then

/**
 * Computes the cross product of the indices [left] and [right].
 * @property left The left index.
 * @property right The right index.
 */
data class Cross<K1 : Comparable<K1>, V1, K2 : Comparable<K2>, V2>(
    val left: Index<K1, V1>,
    val right: Index<K2, V2>
) : Index<Then<K1, K2>, Pair<V1, V2>>() {
    override fun register(
        query: Query<Then<K1, K2>>,
        block: (Then<K1, K2>, Delta<Pair<V1, V2>>) -> Unit
    ): AutoCloseable {
        // Split query.
        val (queryLeft, queryRight) = split(query)

        // Register listener for left.
        val leftListener = left.register(queryLeft) { k1, d ->
            // For all changes on left, pair with matching right elements.
            right.find(queryRight).forEach { (k2, v2) ->
                block(k1 then k2, d.map { it to v2 })
            }
        }

        // Register listener for right.
        val rightListener = right.register(queryRight) { k2, d ->
            // For all changes on right, pair with matching left elements.
            left.find(queryLeft).forEach { (k1, v1) ->
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

        // Find matching parts.
        val fromLeft = left.find(queryLeft)
        val fromRight = right.find(queryRight)

        // Return from left and right via cross product.
        return fromLeft.flatMap { (k1, v1) ->
            fromRight.map { (k2, v2) ->
                (k1 then k2) to (v1 to v2)
            }
        }
    }
}

/**
 * Creates a cross with the operands.
 */
infix fun <K : Comparable<K>, V1, V2> Index<K, V1>.cross(other: Index<K, V2>) =
    Cross(this, other)