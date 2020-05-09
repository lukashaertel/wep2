package eu.metatools.sx.data

import eu.metatools.fio.data.Tri
import java.util.*

// TODO: Re-abstraction
interface Volume<V : Any> {
    /**
     * Puts the value at the coordinate.
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @param z The z-coordinate.
     * @param value The value to put.
     * @return Returns the previous assignment.
     */
    operator fun set(x: Int, y: Int, z: Int, value: V): V?

    /**
     * Removes the value at the coordinate.
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @param z The z-coordinate.
     * @return Returns the previous assignment.
     */
    fun remove(x: Int, y: Int, z: Int): V?

    /**
     * True if that coordinate is assigned.
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @param z The z-coordinate.
     * @return Returns true if assigned.
     */
    fun contains(x: Int, y: Int, z: Int): Boolean

    /**
     * Gets the value at the coordinate.
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @param z The z-coordinate.
     * @return Returns the assignment.
     */
    operator fun get(x: Int, y: Int, z: Int): V?

    /**
     * Gets all values in the coordinate range.
     * @param x The x-coordinate range.
     * @param y The y-coordinate range.
     * @param z The z-coordinate range.
     * @return Returns the assignments.
     */
    operator fun get(x: IntRange, y: IntRange, z: IntRange): Sequence<Pair<Tri, V>>

    /**
     * Gets all assignments.
     * @return Returns all assignments.
     */
    fun getAll(): Sequence<Pair<Tri, V>>

    /**
     * Gets the number of assigned values.
     */
    val size: Int

    /**
     * Gets the estimated center of the volume.
     */
    fun findCenter(): Tri

    /**
     * Sets all non-null values and removes all null values.
     * @param from The values or removals.
     */
    fun assign(from: Map<Tri, V?>) {
        for ((at, value) in from)
            if (value == null)
                remove(at.x, at.y, at.z)
            else
                set(at.x, at.y, at.z, value)
    }

}

/**
 * [Volume.set] with [Tri] coordinate.
 */
operator fun <V : Any> Volume<V>.set(at: Tri, value: V) =
    set(at.x, at.y, at.z, value)

/**
 * [Volume.remove] with [Tri] coordinate.
 */
fun <V : Any> Volume<V>.remove(at: Tri) =
    remove(at.x, at.y, at.z)

/**
 * [Volume.get] with [Tri] coordinate.
 */
operator fun <V : Any> Volume<V>.get(at: Tri) =
    get(at.x, at.y, at.z)


/**
 * [Volume.contains] with [Tri] coordinate.
 */
operator fun <V : Any> Volume<V>.contains(at: Tri) =
    contains(at.x, at.y, at.z)

/**
 * Assigns with a merge operation applied.
 * @param from The value source.
 * @param computeThe merge operation. If the location in [from] has no assignment in the receiver, it will be passed
 * as `null`.
 */
inline fun <V : Any, R : Any> Volume<V>.merge(from: Map<Tri, R>, merge: (V?, R) -> V?) {
    assign(from.mapValues { (at, delta) ->
        merge(get(at.x, at.y, at.z), delta)
    })
}

/**
 * Converts the volume to a map.
 */
fun <V : Any> Volume<V>.toMap() =
    getAll().associate { it }

/**
 * Converts the volume to a sorted map.
 */
fun <V : Any> Volume<V>.toSortedMap() =
    getAll().associateTo(TreeMap()) { it }