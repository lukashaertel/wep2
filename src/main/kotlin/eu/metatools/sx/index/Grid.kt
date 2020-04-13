package eu.metatools.sx.index

import eu.metatools.sx.util.ComparablePair
import eu.metatools.sx.util.toComparable
import java.util.*

/**
 * A store index optimized for a fixed grid.
 * @property width The width of the store.
 * @property height The height of the store.
 */
class StoreGrid<V>(
    val width: Int,
    val height: Int
) : BaseStore<ComparablePair<Int, Int>, V>() {
    companion object {
        /**
         * Creates a query that returns all items left of the given boundary.
         */
        fun left(of: Int) = Before(of toComparable Int.MAX_VALUE)

        /**
         * Creates a query that returns all items above the given boundary.
         */
        fun top(of: Int) = Before(Int.MAX_VALUE toComparable of)

        /**
         * Creates a query that returns all items right of the given boundary.
         */
        fun right(of: Int) = After(of toComparable Int.MIN_VALUE)

        /**
         * Creates a query that returns all items below the given boundary.
         */
        fun down(of: Int) = Before(Int.MIN_VALUE toComparable of)

        /**
         * Creates a query that returns all items within the square.
         */
        fun square(xStart: Int, xEnd: Int, yStart: Int, yEnd: Int) =
            Between(xStart toComparable yStart, xEnd toComparable yEnd)

        /**
         * Creates a square around the coordinates with the given radii.
         */
        fun around(x: Int, y: Int, width: Int, height: Int) =
            square(x - width, x + width + 1, y - height, y + height + 1)
    }

    /**
     * The items that are stored in this grid index.
     */
    //private val items = Array(height) { Array<Option<V>>(width) { None() } }

    private val items = arrayOfNulls<Any>(width * height)
    private val assigned = BitSet(width * height)

    /**
     * Gets the item at the given array position, assumes exists as V.
     */
    @Suppress("unchecked_cast", "nothing_to_inline")
    private inline fun getItem(index: Int) =
        items[index] as V

    override fun put(key: ComparablePair<Int, Int>, value: V): V? {
        // Get index and check if value has assignment.
        val index = key.second * width + key.first
        val exists = assigned[index]

        // Disambiguate on previous value present.
        if (exists) {
            // Get previous value.
            val previous = getItem(index)

            // Override value.
            items[index] = value

            // On true change, publish change.
            if (previous != value)
                publish(key, DeltaChange(previous, value))

            // Return previous value.
            return previous
        } else {
            // Set assignment to true and assign value.
            assigned[index] = true
            items[index] = value

            // Publish delta add.
            publish(key, DeltaAdd(value))

            // Return null, no previous assignment.
            return null
        }
    }

    /**
     * Shorthand for `put(x toComparable y, value)`
     */
    fun put(x: Int, y: Int, value: V) =
        put(x toComparable y, value)

    override fun remove(key: ComparablePair<Int, Int>): V? {
        // Get index and check if value has assignment.
        val index = key.second * width + key.first
        val exists = assigned[index]

        if (!exists)
            return null

        // Get previous value.
        val previous = getItem(index)

        // Tag not existing.
        assigned[index] = false

        // Publish remove.
        publish(key, DeltaRemove(previous))

        // Return previous assignment.
        return previous
    }

    /**
     * Shorthand for `remove(x toComparable y)`
     */
    fun remove(x: Int, y: Int) =
        remove(x toComparable y)

    override fun find(query: Query<ComparablePair<Int, Int>>): Sequence<Pair<ComparablePair<Int, Int>, V>> {
        // Split the queries.
        val (queryX, queryY) = split(query)

        /**
         * Clamps a horizontal component.
         */
        fun clampWidth(x: Int) = minOf(width, maxOf(0, x))

        /**
         * Clamps a vertical component.
         */
        fun clampHeight(y: Int) = minOf(height, maxOf(0, y))

        // Compute the sequence of x-coordinates.
        val xs = when (queryX) {
            is Always -> (0 until width).asSequence()
            is Never -> emptySequence()

            is At -> when {
                queryX.key < 0 -> emptySequence()
                width <= queryX.key -> emptySequence()
                else -> sequenceOf(queryX.key)
            }

            is After -> (clampWidth(queryX.key) until width).asSequence()
            is Before -> (0 until clampWidth(queryX.key)).asSequence()
            is Between -> (clampWidth(queryX.keyLower) until clampWidth(queryX.keyUpper)).asSequence()
        }

        // Compute the sequence of y-coordinates.
        val ys = when (queryY) {
            is Always -> (0 until width).asSequence()
            is Never -> emptySequence()

            is At -> when {
                queryY.key < 0 -> emptySequence()
                height <= queryY.key -> emptySequence()
                else -> sequenceOf(queryY.key)
            }

            is After -> (clampHeight(queryY.key) until width).asSequence()
            is Before -> (0 until clampHeight(queryY.key)).asSequence()
            is Between -> (clampHeight(queryY.keyLower) until clampHeight(queryY.keyUpper)).asSequence()
        }

        // Return for the coordinates all items that are present.
        return ys.flatMap { y ->
            val offset = y * width
            xs.mapNotNull { x ->
                val index = offset + x
                if (assigned[index])
                    (x toComparable y) to getItem(index)
                else
                    null
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StoreGrid<*>

        if (width != other.width) return false
        if (height != other.height) return false
        if (!items.contentDeepEquals(other.items)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + items.contentDeepHashCode()
        return result
    }
}

fun main() {
    val gs = StoreGrid<String>(10, 10)
    gs.register(After(5 toComparable 0)) { k, d ->
        println("$k $d")
    }
    gs.put(2, 0, "x")
    gs.put(6, 0, "y")
    gs.put(6, 4, "z")
    gs.put(6, 6, "w")
    gs.put(6, 9, "u")
    gs.find(StoreGrid.square(3, 8, 3, 8)).forEach { println(it) }
    gs.put(6, 9, "a")
    gs.remove(6, 9)
}