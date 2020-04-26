package eu.metatools.sx.data

import eu.metatools.fio.data.Tri
import java.util.*

class ObservedVolume<V : Any>(
    val actual: Volume<V>,
    val notify: (add: SortedMap<Tri, V>, remove: SortedMap<Tri, V>) -> Unit
) : Volume<V> {
    override fun set(x: Int, y: Int, z: Int, value: V): V? {
        val previous = actual.set(x, y, z, value)
        if (previous == null)
            notify(sortedMapOf(Tri(x, y, z) to value), sortedMapOf())
        else
            notify(sortedMapOf(Tri(x, y, z) to value), sortedMapOf(Tri(x, y, z) to previous))

        return previous
    }

    override fun remove(x: Int, y: Int, z: Int): V? {
        val previous = actual.remove(x, y, z)
        if (previous != null)
            notify(sortedMapOf(), sortedMapOf(Tri(x, y, z) to previous))
        return previous
    }

    override fun contains(x: Int, y: Int, z: Int) =
        actual.contains(x, y, z)

    override fun get(x: Int, y: Int, z: Int) =
        actual[x, y, z]

    override fun get(x: IntRange, y: IntRange, z: IntRange) =
        actual[x, y, z]

    override fun getAll() =
        actual.getAll()

    override val size: Int
        get() = actual.size

    override fun findCenter() =
        actual.findCenter()

    override fun assign(from: Map<Tri, V?>) {
        val remove = TreeMap<Tri, V>()
        val add = TreeMap<Tri, V>()

        // Iterate assignments.
        for ((at, value) in from)
        /// Check if removal or addition.
            if (value == null) {
                // Removal, find previous value from removing.
                val previous = actual.remove(at.x, at.y, at.z)

                // Previous value assigned, add to removal.
                if (previous != null)
                    remove[at] = previous
            } else {
                // Addition or overwrite, find previous value from setting.
                val previous = actual.set(at.x, at.y, at.z, value)

                // Check if previous value unassigned.
                if (previous == null) {
                    // Unassigned, add to addition.
                    add[at] = value
                } else if (previous != value) {
                    // Add new value to addition and old value to removal.
                    add[at] = value
                    remove[at] = previous
                }
            }

        notify(add, remove)
    }

    override fun hashCode() =
        actual.hashCode()

    override fun equals(other: Any?) =
        actual == other

    override fun toString() =
        actual.toString()
}