package eu.metatools.sx.index

import eu.metatools.fio.data.Tri
import java.util.*

/**
 * An index representing a sparsely populated, queryable volume.
 * @property dimension The chunk side dimensions per block
 */
class IndexVolume<V>(val dimension: Int = 8) : BaseIndex<Tri, V>() {
    companion object {
        /**
         * Returns the elements with x less than the given [limit].
         */
        fun left(limit: Int) = Before(Tri(limit.dec(), Int.MAX_VALUE, Int.MAX_VALUE))

        /**
         * Returns the element directly left of [tri].
         */
        fun left(tri: Tri) = At(Tri(tri.x.dec(), tri.y, tri.z))

        /**
         * Returns the elements with y less than the given [limit].
         */
        fun back(limit: Int) = Before(Tri(Int.MAX_VALUE, limit.dec(), Int.MAX_VALUE))

        /**
         * Returns the element directly behind of [tri].
         */
        fun back(tri: Tri) = At(Tri(tri.x, tri.y.dec(), tri.z))

        /**
         * Returns the elements with z less than the given [limit].
         */
        fun below(limit: Int) = Before(Tri(Int.MAX_VALUE, Int.MAX_VALUE, limit.dec()))

        /**
         * Returns the element directly below of [tri].
         */
        fun below(tri: Tri) = At(Tri(tri.x, tri.y, tri.z.dec()))

        /**
         * Returns the elements with x greater than the given [limit].
         */
        fun right(limit: Int) = After(Tri(limit.inc(), Int.MIN_VALUE, Int.MIN_VALUE))

        /**
         * Returns the element directly right of [tri].
         */
        fun right(tri: Tri) = At(Tri(tri.x.inc(), tri.y, tri.z))

        /**
         * Returns the elements with y greater than the given [limit].
         */
        fun front(limit: Int) = After(Tri(Int.MIN_VALUE, limit.inc(), Int.MIN_VALUE))

        /**
         * Returns the element directly in front of [tri].
         */
        fun front(tri: Tri) = At(Tri(tri.x, tri.y.inc(), tri.z))

        /**
         * Returns the elements with z greater than the given [limit].
         */
        fun above(limit: Int) = After(Tri(Int.MIN_VALUE, Int.MIN_VALUE, limit.inc()))

        /**
         * Returns the element directly above [tri].
         */
        fun above(tri: Tri) = At(Tri(tri.x, tri.y, tri.z.inc()))

        /**
         * Creates a query that returns all items within the cube, performs orientation checks and picks the minimum
         * and maximum coordinates from [first] and [second] accordingly. The endpoint is inclusive.
         */
        fun within(first: Tri, second: Tri) =
            Between(
                Tri(minOf(first.x, second.x), minOf(first.y, second.y), minOf(first.z, second.z)),
                Tri(maxOf(first.x, second.x), maxOf(first.y, second.y), maxOf(first.z, second.z))
            )

        /**
         * Creates a cube around the given coordinates collecting the immediate neighbours.
         */
        fun around(at: Tri) =
            Between(
                Tri(at.x.dec(), at.y.dec(), at.z.dec()),
                Tri(at.x.inc(), at.y.inc(), at.z.inc())
            )

        /**
         * Creates a cube around the given coordinates collecting the immediate neighbours.
         */
        fun around(x: Int, y: Int, z: Int) =
            Between(
                Tri(x.dec(), y.dec(), z.dec()),
                Tri(x.inc(), y.inc(), z.inc())
            )

        /**
         * Creates a cube around the given coordinates with the given dimensions.
         */
        fun around(at: Tri, size: Tri) =
            Between(
                Tri(at.x - size.x, at.y - size.y, at.z - size.z),
                Tri(at.x + size.x, at.y + size.y, at.z + size.z)
            )

        /**
         * Creates a cube around the given coordinates with the given dimensions.
         */
        fun around(x: Int, y: Int, z: Int, sx: Int, sy: Int = sx, sz: Int = sx) =
            Between(
                Tri(x - sx, y - sy, z - sz),
                Tri(x + sx, y + sy, z + sz)
            )
    }

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
            index in elements.indices && assigned[index]

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

    private var hash: Int? = null

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
                // Was assigned, publish as change if actually changed.
                if (it != value) {
                    hash = null
                    publish(key, DeltaChange(it as V, value))
                }
            }
        else
            chunk.set(li, value).also {
                // Not assigned, publish as add.
                hash = null
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
        hash = null
        publish(key, DeltaRemove(result as V))

        // Return old value.
        return result
    }

    /**
     * Shorthand for `remove(Tri(x, y, z))`
     */
    fun remove(x: Int, y: Int, z: Int) =
        remove(Tri(x, y, z))

    override fun find(query: Query<Tri>): Sequence<Pair<Tri, V>> =
        when (query) {
            is Always -> findAlways()
            is Never -> findNever()
            is At -> findAt(query)
            is After -> findAfter(query)
            is Before -> findBefore(query)
            is Between -> findBetween(query)
        }

    /**
     * Finds all elements.
     */
    private fun findAlways(): Sequence<Pair<Tri, V>> {
        // All inner indices.
        val innerIndices = (0 until dimension).asSequence()

        // Iterate chunks and their elements.
        return chunks.asSequence().flatMap { (uz, zs) ->
            val oz = uz * dimension
            zs.asSequence().flatMap { (uy, ys) ->
                val oy = uy * dimension
                ys.asSequence().flatMap { (ux, xs) ->
                    val ox = ux * dimension
                    var index = 0
                    innerIndices.flatMap { z ->
                        innerIndices.flatMap { y ->
                            innerIndices.mapNotNull { x ->
                                val ci = index++
                                if (ci in xs)
                                    Tri(ox + x, oy + y, oz + z) to xs[ci]
                                else
                                    null
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Finds no elements, returning empty sequence.
     */
    private fun findNever(): Sequence<Pair<Tri, V>> =
        emptySequence()

    /**
     * Finds exact match.
     */
    private fun findAt(query: At<Tri>): Sequence<Pair<Tri, V>> {
        // Get upper indices.
        val ux = query.key.x / dimension
        val uy = query.key.y / dimension
        val uz = query.key.z / dimension

        // Load or create chunk.
        val chunk = ((chunks[uz] ?: return emptySequence())[uy] ?: return emptySequence())[ux] ?: return emptySequence()

        // Get lower index.
        val lx = query.key.x - ux * dimension
        val ly = query.key.y - uy * dimension
        val lz = query.key.z - uz * dimension
        val li = lz * dimension * dimension + ly * dimension + lx

        // Check if assigned in chunk, return sequence of assignment if present.
        return if (li in chunk)
            sequenceOf(query.key to chunk[li])
        else
            emptySequence()
    }

    /**
     * Finds lower bounded elements.
     */
    private fun findAfter(query: After<Tri>): Sequence<Pair<Tri, V>> {
        val chunkLimitZ = query.key.z / dimension
        val chunkLimitY = query.key.y / dimension
        val chunkLimitX = query.key.x / dimension

        return chunks.tailMap(chunkLimitZ, true).asSequence().flatMap { (uz, zs) ->
            // z-coordinate to chunk base and chunk indices.
            val oz = uz * dimension
            val iz = maxOf(0, query.key.z - oz) until dimension

            // Iterate y-chunks.
            zs.tailMap(chunkLimitY, true).asSequence().flatMap { (uy, ys) ->
                // y-coordinate to chunk base and chunk indices.
                val oy = uy * dimension
                val iy = maxOf(0, query.key.y - oy) until dimension

                // Iterate x-chunks.
                ys.tailMap(chunkLimitX, true).asSequence().flatMap { (ux, xs) ->
                    // x-coordinate to chunk base and chunk indices.
                    val ox = ux * dimension
                    val ix = maxOf(0, query.key.x - ox) until dimension

                    // Iterate chunk elements themselves.
                    iz.asSequence().flatMap { z ->
                        val ioz = z * dimension * dimension
                        iy.asSequence().flatMap { y ->
                            val ioy = ioz + y * dimension
                            ix.asSequence().mapNotNull { x ->
                                // Compute carried index.
                                val iox = ioy + x

                                // If index is assigned, return item, otherwise return null.
                                if (iox in xs)
                                    Tri(ox + x, oy + y, oz + z) to xs[iox]
                                else
                                    null
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Finds upper bounded elements.
     */
    private fun findBefore(query: Before<Tri>): Sequence<Pair<Tri, V>> {
        val chunkLimitZ = query.key.z / dimension
        val chunkLimitY = query.key.y / dimension
        val chunkLimitX = query.key.x / dimension

        return chunks.headMap(chunkLimitZ, true).asSequence().flatMap { (uz, zs) ->
            // z-coordinate to chunk base and chunk indices.
            val oz = uz * dimension
            val iz = 0..minOf(dimension.dec(), query.key.z - oz)

            // Iterate y-chunks.
            zs.headMap(chunkLimitY, true).asSequence().flatMap { (uy, ys) ->
                // y-coordinate to chunk base and chunk indices.
                val oy = uy * dimension
                val iy = 0..minOf(dimension.dec(), query.key.y - oy)

                // Iterate x-chunks.
                ys.headMap(chunkLimitX, true).asSequence().flatMap { (ux, xs) ->
                    // x-coordinate to chunk base and chunk indices.
                    val ox = ux * dimension
                    val ix = 0..minOf(dimension.dec(), query.key.x - ox)

                    // Iterate chunk elements themselves.
                    iz.asSequence().flatMap { z ->
                        val ioz = z * dimension * dimension
                        iy.asSequence().flatMap { y ->
                            val ioy = ioz + y * dimension
                            ix.asSequence().mapNotNull { x ->
                                // Compute carried index.
                                val iox = ioy + x

                                // If index is assigned, return item, otherwise return null.
                                if (iox in xs)
                                    Tri(ox + x, oy + y, oz + z) to xs[iox]
                                else
                                    null
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Finds bounded elements.
     */
    private fun findBetween(query: Between<Tri>): Sequence<Pair<Tri, V>> {
        val chunkLimitLowerZ = query.keyLower.z / dimension
        val chunkLimitLowerY = query.keyLower.y / dimension
        val chunkLimitLowerX = query.keyLower.x / dimension
        val chunkLimitUpperZ = query.keyUpper.z / dimension
        val chunkLimitUpperY = query.keyUpper.y / dimension
        val chunkLimitUpperX = query.keyUpper.x / dimension

        return chunks.subMap(chunkLimitLowerZ, true, chunkLimitUpperZ, true).asSequence().flatMap { (uz, zs) ->
            // z-coordinate to chunk base and chunk indices.
            val oz = uz * dimension
            val iz = maxOf(0, query.keyLower.z - oz)..minOf(dimension.dec(), query.keyUpper.z - oz)

            // Iterate y-chunks.
            zs.subMap(chunkLimitLowerY, true, chunkLimitUpperY, true).asSequence().flatMap { (uy, ys) ->
                // y-coordinate to chunk base and chunk indices.
                val oy = uy * dimension
                val iy = maxOf(0, query.keyLower.y - oy)..minOf(dimension.dec(), query.keyUpper.y - oy)

                // subMap x-chunks.
                ys.subMap(chunkLimitLowerX, true, chunkLimitUpperX, true).asSequence().flatMap { (ux, xs) ->
                    // x-coordinate to chunk base and chunk indices.
                    val ox = ux * dimension
                    val ix = maxOf(0, query.keyLower.x - ox)..minOf(dimension.dec(), query.keyUpper.x - ox)

                    // Iterate chunk elements themselves.
                    iz.asSequence().flatMap { z ->
                        val ioz = z * dimension * dimension
                        iy.asSequence().flatMap { y ->
                            val ioy = ioz + y * dimension
                            ix.asSequence().mapNotNull { x ->
                                // Compute carried index.
                                val iox = ioy + x

                                // If index is assigned, return item, otherwise return null.
                                if (iox in xs)
                                    Tri(ox + x, oy + y, oz + z) to xs[iox]
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
        // Try returning existing hash.
        val existing = hash
        if (existing != null)
            return existing

        // Compute new hash.
        var new = dimension
        new = 31 * new + chunks.hashCode()

        // Assign and return new hash.
        hash = new
        return new
    }


}

fun main() {
    val gs = IndexVolume<List<Int>>()
    val f = gs.flatten()
    f.register(Always()) { k, d ->
        println("$k: $d")
    }

    gs.put(0, 0, 0, listOf(1, 2))

    println(gs.find(Before(Tri(10, 10, 10))).toList())
}