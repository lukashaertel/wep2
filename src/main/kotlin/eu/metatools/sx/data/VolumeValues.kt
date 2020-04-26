package eu.metatools.sx.data

import eu.metatools.fio.data.Tri
import java.util.*

/**
 * Volume of value assignments.
 */
class VolumeValues<V : Any>(val chunkSize: Int = 8) : Volume<V> {
    companion object {
        @Suppress("nothing_to_inline")
        private inline infix fun Int.dz(other: Int) =
            Math.floorDiv(this, other)
    }

    /**
     * Local sparse chunk.
     */
    private class Chunk<V>(chunkSize: Int) {
        private val limit = chunkSize * chunkSize * chunkSize

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
        var size = 0
            private set

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
                size++
                null
            }

        /**
         * Removes the value, updates assignedness.
         */
        fun remove(index: Int) =
            if (assigned[index]) {
                val previous = get(index)
                assigned[index] = false
                size--
                previous
            } else {
                null
            }

        /**
         * True if empty.
         */
        fun isEmpty() =
            size == 0
    }

    /**
     * Chunks of assignments.
     */
    private val chunks = TreeMap<Int, TreeMap<Int, TreeMap<Int, Chunk<V>>>>()

    /**
     * The computed hash or null if reset.
     */
    private var hash: Int? = null

    /**
     * The computed center or null if reset.
     */
    private var center: Tri? = null

    override var size = 0
        private set

    override fun set(x: Int, y: Int, z: Int, value: V): V? {
        // Get upper indices.
        val ux = x dz chunkSize
        val uy = y dz chunkSize
        val uz = z dz chunkSize

        // Load or create chunk.
        val chunk = chunks.getOrPut(uz, ::TreeMap).getOrPut(uy, ::TreeMap).getOrPut(ux) { Chunk(chunkSize) }

        // Get lower index.
        val lx = x - ux * chunkSize
        val ly = y - uy * chunkSize
        val lz = z - uz * chunkSize
        val li = lz * chunkSize * chunkSize + ly * chunkSize + lx

        // Check if assigned in chunk.
        return if (li in chunk)
            chunk.set(li, value).also {
                // Was assigned, reset hash if value changed.
                if (it != value)
                    hash = null
            }
        else
            chunk.set(li, value).also {
                // Not assigned, reset hash.
                hash = null
                center = null
                size++
            }

    }

    override fun remove(x: Int, y: Int, z: Int): V? {
        // Get upper indices.
        val ux = x dz chunkSize
        val uy = y dz chunkSize
        val uz = z dz chunkSize

        // Load chunk or stop.
        val ty = chunks[uz]
        val tx = ty?.get(uy)
        val chunk = tx?.get(ux) ?: return null

        // Get lower index.
        val lx = x - ux * chunkSize
        val ly = y - uy * chunkSize
        val lz = z - uz * chunkSize
        val li = lz * chunkSize * chunkSize + ly * chunkSize + lx

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

        // Reset hash.
        hash = null
        center = null
        size--

        // Return old value.
        return result
    }

    override fun contains(x: Int, y: Int, z: Int): Boolean {
        // Get upper indices.
        val ux = x dz chunkSize
        val uy = y dz chunkSize
        val uz = z dz chunkSize

        // Load or return false.
        val chunk = ((chunks[uz] ?: return false)[uy] ?: return false)[ux] ?: return false

        // Get lower index.
        val lx = x - ux * chunkSize
        val ly = y - uy * chunkSize
        val lz = z - uz * chunkSize
        val li = lz * chunkSize * chunkSize + ly * chunkSize + lx

        // Return if assigned in chunk.
        return (li in chunk)
    }

    override fun get(x: Int, y: Int, z: Int): V? {
        // Get upper indices.
        val ux = x dz chunkSize
        val uy = y dz chunkSize
        val uz = z dz chunkSize

        // Load or return null.
        val chunk = ((chunks[uz] ?: return null)[uy] ?: return null)[ux] ?: return null

        // Get lower index.
        val lx = x - ux * chunkSize
        val ly = y - uy * chunkSize
        val lz = z - uz * chunkSize
        val li = lz * chunkSize * chunkSize + ly * chunkSize + lx

        // Check if assigned in chunk, return value or null.
        return if (li in chunk)
            chunk[li]
        else
            null
    }

    override fun get(x: IntRange, y: IntRange, z: IntRange): Sequence<Pair<Tri, V>> {
        val chunkLimitLowerZ = z.first dz chunkSize
        val chunkLimitLowerY = y.first dz chunkSize
        val chunkLimitLowerX = x.first dz chunkSize
        val chunkLimitUpperZ = z.last dz chunkSize
        val chunkLimitUpperY = y.last dz chunkSize
        val chunkLimitUpperX = x.last dz chunkSize

        // TODO: Better solution for under/overflow.

        return chunks.subMap(chunkLimitLowerZ, true, chunkLimitUpperZ, true).asSequence().flatMap { (uz, zs) ->
            // z-coordinate to chunk base and chunk indices.
            val oz = uz * chunkSize
            val izl = maxOf(0, z.first.toLong() - oz).toInt()
            val izu = minOf(chunkSize.dec().toLong(), z.last.toLong() - oz).toInt()
            val iz = izl..izu

            // Iterate y-chunks.
            zs.subMap(chunkLimitLowerY, true, chunkLimitUpperY, true).asSequence().flatMap { (uy, ys) ->
                // y-coordinate to chunk base and chunk indices.
                val oy = uy * chunkSize
                val iyl = maxOf(0L, y.first.toLong() - oy).toInt()
                val iyu = minOf(chunkSize.dec().toLong(), y.last.toLong() - oy).toInt()
                val iy = iyl..iyu

                // subMap x-chunks.
                ys.subMap(chunkLimitLowerX, true, chunkLimitUpperX, true).asSequence().flatMap { (ux, xs) ->
                    // x-coordinate to chunk base and chunk indices.
                    val ox = ux * chunkSize
                    val ixl = maxOf(0L, x.first.toLong() - ox).toInt()
                    val ixu = minOf(chunkSize.dec().toLong(), x.last.toLong() - ox).toInt()
                    val ix = ixl..ixu

                    // Iterate chunk elements themselves.
                    iz.asSequence().flatMap { z ->
                        val ioz = z * chunkSize * chunkSize
                        iy.asSequence().flatMap { y ->
                            val ioy = ioz + y * chunkSize
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

    override fun getAll(): Sequence<Pair<Tri, V>> {
        // All inner indices.
        val innerIndices = (0 until chunkSize).asSequence()

        // Iterate chunks and their elements.
        return chunks.asSequence().flatMap { (uz, zs) ->
            val oz = uz * chunkSize
            zs.asSequence().flatMap { (uy, ys) ->
                val oy = uy * chunkSize
                ys.asSequence().flatMap { (ux, xs) ->
                    val ox = ux * chunkSize
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

    override fun findCenter(): Tri {
        val existing = center
        if (existing != null)
            return existing

        // Initialize aggregators.
        var amount = 0
        var cx = 0
        var cy = 0
        var cz = 0

        // Aggregate weighted.
        for ((uz, zs) in chunks)
            for ((uy, ys) in zs)
                for ((ux, xs) in ys) {
                    amount += xs.size
                    cx += ux * xs.size
                    cy += uy * xs.size
                    cz += uz * xs.size
                }

        // Set to actual center, always same offset.
        val new = Tri(cx + (chunkSize / 2), cy + (chunkSize / 2), cz + (chunkSize / 2))

        // Assign and return new center.
        center = new
        return new
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VolumeValues<*>

        if (chunkSize != other.chunkSize) return false
        if (chunks != other.chunks) return false

        return true
    }

    override fun hashCode(): Int {
        // Try returning existing hash.
        val existing = hash
        if (existing != null)
            return existing

        // Compute new hash.
        var new = chunkSize
        new = 31 * new + chunks.hashCode()

        // Assign and return new hash.
        hash = new
        return new
    }

    override fun toString() =
        if (size > 10)
            "Volume {" + getAll().take(10).joinToString() + ", ... }"
        else
            "Volume {" + getAll().joinToString() + "}"
}