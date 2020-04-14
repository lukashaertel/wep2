package eu.metatools.sx.index

import eu.metatools.fio.data.Tri
import eu.metatools.up.dt.div
import eu.metatools.up.dt.lx
import java.util.*

/**
 * A store index optimized for a fixed grid.
 * @property dimension The chunk size per block
 */
class IndexVolume<V>(val dimension: Int = 32) : BaseIndex<Tri, V>() {
//    companion object {
//        /**
//         * Creates a query that returns all items left of the given boundary.
//         */
//        fun left(of: Int) = Before(lx / of / Int.MAX_VALUE)
//
//        /**
//         * Creates a query that returns all items above the given boundary.
//         */
//        fun top(of: Int) = Before(lx / Int.MAX_VALUE / of)
//
//        /**
//         * Creates a query that returns all items right of the given boundary.
//         */
//        fun right(of: Int) = After(lx / of / Int.MIN_VALUE)
//
//        /**
//         * Creates a query that returns all items below the given boundary.
//         */
//        fun down(of: Int) = Before(lx / Int.MIN_VALUE / of)
//
//        /**
//         * Creates a query that returns all items within the square.
//         */
//        fun square(xStart: Int, xEnd: Int, yStart: Int, yEnd: Int) =
//            Between(lx / xStart / yStart, lx / xEnd / yEnd)
//
//        /**
//         * Creates a square around the coordinates with the given radii.
//         */
//        fun around(x: Int, y: Int, width: Int, height: Int) =
//            square(x - width, x + width + 1, y - height, y + height + 1)
//    }

    /**
     * Local sparse chunk.
     */
    private class Chunk<V>(dimension: Int) {
        private val limit = dimension * dimension * dimension

        /**
         * Elements of the chunk.
         */
        private val elements = arrayOfNulls<Any>(limit)

        /**
         * True if the element is assigned.
         */
        private val assigned = BitSet(limit)

        /**
         * Counted cardinality.
         */
        private var cardinality = 0

        /**
         * True if index is assigned.
         */
        operator fun contains(index: Int) =
            0 <= index && index < limit && assigned[index]

        /**
         * Gets the item at the index, does not check assignedness.
         */
        @Suppress("unchecked_cast")
        operator fun get(index: Int) =
            elements[index] as V

        /**
         * Sets the value, updates assignedness.
         */
        operator fun set(index: Int, value: V) =
            if (assigned[index]) {
                // Was assigned, get previous value and update.
                val previous = get(index)
                elements[index] = value
                previous
            } else {
                // Was not assigned, enable and write.
                assigned[index] = true
                elements[index] = value
                cardinality++
                null
            }

        /**
         * Removes the value, updates assignedness.
         */
        fun remove(index: Int) =
            if (assigned[index]) {
                val previous = get(index)
                assigned[index] = false
                cardinality--
                previous
            } else {
                null
            }

        /**
         * Gets the amount of assigned items.
         */
        fun count() =
            cardinality

        /**
         * True if empty.
         */
        fun isEmpty() =
            cardinality == 0

        /**
         * Gets all elements as a list.
         */
        fun toList() =
            elements.filterIndexed { index, _ -> assigned[index] } as List<V>

        /**
         * Gets all elements as a sequence.
         */
        fun asSequence() =
            elements.asSequence().filterIndexed { index, _ -> assigned[index] } as Sequence<V>
    }

    /**
     * The items that are stored in this grid index.
     */
    private val chunks = TreeMap<Int, TreeMap<Int, TreeMap<Int, Chunk<V>>>>()

    override fun put(key: Tri, value: V): V? {
        // Get upper indices.
        val ux = key.x / dimension
        val uy = key.y / dimension
        val uz = key.z / dimension

        // Load or create chunk.
        val chunk = chunks.getOrPut(uz, ::TreeMap).getOrPut(uy, ::TreeMap).getOrPut(ux) { Chunk(dimension) }

        // Get lower index.
        val lx = key.x - ux * dimension
        val ly = key.y - uy * dimension
        val lz = key.z - uz * dimension
        val li = lz * dimension * dimension + ly * dimension + lx

        // Check if assigned in chunk.
        return if (li in chunk)
            chunk.set(li, value).also {
                // Was assigned, publish as change.
                publish(key, DeltaChange(it as V, value))
            }
        else
            chunk.set(li, value).also {
                // Not assigned, publish as add.
                publish(key, DeltaAdd(value))
            }

    }

    /**
     * Shorthand for `put(Tri(x, y, z), value)`
     */
    fun put(x: Int, y: Int, z: Int, value: V) =
        put(Tri(x, y, z), value)

    override fun remove(key: Tri): V? {
        // Get upper indices.
        val ux = key.x / dimension
        val uy = key.y / dimension
        val uz = key.z / dimension

        // Load chunk or stop.
        val ty = chunks[uz]
        val tx = ty?.get(uy)
        val chunk = tx?.get(ux) ?: return null

        // Get lower index.
        val lx = key.x - ux * dimension
        val ly = key.y - uy * dimension
        val lz = key.z - uz * dimension
        val li = lz * dimension * dimension + ly * dimension + lx

        // Not in chunk, stop.
        if (li !in chunk)
            return null

        // Remove for result, update chunk mappings.
        val result = chunk.remove(li)
        if (chunk.isEmpty())
            tx.remove(ux)
        if (tx.isEmpty())
            ty.remove(uy)
        if (ty.isEmpty())
            chunks.remove(uz)

        // Publish change.
        publish(key, DeltaRemove(result as V))

        // Return old value.
        return result
    }

    /**
     * Shorthand for `remove(Tri(x, y, z))`
     */
    fun remove(x: Int, y: Int, z: Int) =
        remove(Tri(x, y, z))

    override fun find(query: Query<Tri>): Sequence<Pair<Tri, V>> {
        // Convert query to absolute indices.
        val qx: IntRange
        val qy: IntRange
        val qz: IntRange

        when (query) {
            is Always -> {
                qx = Int.MIN_VALUE until Int.MAX_VALUE
                qy = Int.MIN_VALUE until Int.MAX_VALUE
                qz = Int.MIN_VALUE until Int.MAX_VALUE
            }
            is Never -> {
                qx = 0 until 0
                qy = 0 until 0
                qz = 0 until 0
            }
            is At -> {
                qx = query.key.x..query.key.x
                qy = query.key.y..query.key.y
                qz = query.key.z..query.key.z
            }
            is After -> {
                qx = query.key.x until Int.MAX_VALUE
                qy = query.key.y until Int.MAX_VALUE
                qz = query.key.z until Int.MAX_VALUE
            }
            is Before -> {
                qx = Int.MIN_VALUE until query.key.x
                qy = Int.MIN_VALUE until query.key.y
                qz = Int.MIN_VALUE until query.key.z
            }
            is Between -> {
                qx = query.keyLower.x until query.keyUpper.x
                qy = query.keyLower.y until query.keyUpper.y
                qz = query.keyLower.z until query.keyUpper.z
            }
        }

        // Map to chunk index.
        val rx = qx.first / dimension..qx.last / dimension
        val ry = qy.first / dimension..qy.last / dimension
        val rz = qz.first / dimension..qz.last / dimension

        // Iterate z-chunks.
        return chunks.subMap(rz.first, true, rz.last, true).asSequence().flatMap { (uz, zs) ->
            // z-coordinate to chunk base and chunk indices.
            val oz = uz * dimension
            val iz = maxOf(0, qz.first - oz) until minOf(dimension, qz.last - oz)

            // Iterate y-chunks.
            zs.subMap(ry.first, true, ry.last, true).asSequence().flatMap { (uy, ys) ->
                // y-coordinate to chunk base and chunk indices.
                val oy = uy * dimension
                val iy = maxOf(0, qy.first - oy) until minOf(dimension, qy.last - oy)

                // Iterate x-chunks.
                ys.subMap(rx.first, true, rx.last, true).asSequence().flatMap { (ux, xs) ->
                    // x-coordinate to chunk base and chunk indices.
                    val ox = ux * dimension
                    val ix = maxOf(0, qx.first - ox) until minOf(dimension, qx.last - ox)

                    // Iterate chunk elements themselves.
                    iz.asSequence().flatMap { z ->
                        val a = z * dimension * dimension
                        iy.asSequence().flatMap { y ->
                            val b = a + y * dimension
                            ix.asSequence().mapNotNull { x ->
                                // Compute carried index.
                                val index = b + x

                                // If index is assigned, return item, otherwise return null.
                                if (index in xs)
                                    Tri(ox + x, oy + y, oz + z) to xs[index]
                                else
                                    null
                            }
                        }
                    }
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IndexVolume<*>

        if (dimension != other.dimension) return false
        if (chunks != other.chunks) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dimension
        result = 31 * result + chunks.hashCode()
        return result
    }


}

fun main() {
    val gs = IndexVolume<String>(4)
    println(After(Tri(5, 0, 0))(Tri(6, 4, 0)))
    gs.register(After(Tri(5, 0, 0))) { k, d ->
        println("L: $k $d")
    }
    gs.put(2, 0, 0, "x")
    gs.put(6, 0, 0, "y")
    gs.put(6, 4, 0, "z")
    gs.put(6, 6, 0, "w")
    gs.put(6, 9, 0, "u")
    gs.find(Always()).forEach { println("A: $it") }
    gs.find(After(Tri(3, 4, 0))).forEach { println("Q: $it") }
    gs.put(6, 9, 0, "a")
    gs.remove(6, 9, 0)
}