package eu.metatools.up.dt

import eu.metatools.up.lang.continuedBy
import eu.metatools.up.lang.validate

/**
 * A local node in the [Lx] sequence.
 */
sealed class Local {
    /**
     * If true, this object does not represent a concrete coordinate but a conceptual boundary.
     */
    abstract val isAbstract: Boolean
}

/**
 * Object that is less then every other.
 */
object Inf : Local() {
    override val isAbstract = true

    override fun toString() = "inf"
}

/**
 * Object that is greater then every other.
 */
object Sup : Local() {
    override val isAbstract = true

    override fun toString() = "sup"
}

/**
 * Exact object coordinate.
 * @property value The value of this coordinate.
 */
data class At<T : Comparable<T>>(val value: T) : Local() {
    override val isAbstract = false

    override fun toString() =
        value.toString()
}

fun Local.requireValue() = requireNotNull(this as? At<*>).value

/**
 * Lexicographic hierarchy supporting [Inf] and [Sup] queries.
 */
class Lx(val nodes: List<Local>) : Comparable<Lx>, List<Local> by nodes {
    /**
     * If true, any of the [nodes] is a conceptual boundary ([Local.isAbstract]).
     */
    val isAbstract
        get() = nodes.any(Local::isAbstract)

    override fun compareTo(other: Lx): Int {
        // Same object, return immediately.
        if (this === other)
            return 0

        // Compare the node steps individually.
        for (i in 0 until minOf(nodes.size, other.nodes.size)) {
            val a = nodes[i]
            val b = other.nodes[i]

            // Handle if this side is a boundary value.
            if (a == Inf)
                return if (b == Inf) 0 else Int.MIN_VALUE
            else if (a == Sup)
                return if (b == Sup) 0 else Int.MAX_VALUE

            // Handle if other side is boundary value.
            if (b == Inf)
                return Int.MAX_VALUE
            else if (b == Sup)
                return Int.MIN_VALUE

            // No component is a boundary value, assert their types already.
            a as At<*>
            b as At<*>

            // Get types of the local IDs.
            val alphaType = a.value::class
            val betaType = b.value::class

            // If types are the same, compare the local IDs, otherwise use their classes as proxies.
            val byLocal = if (alphaType == betaType)
                compareValuesBy(a.value, b.value) { it }
            else
                compareValuesBy(alphaType, betaType) { it.toString() }

            // If non-zero result at the node, return the value.
            if (byLocal != 0)
                return byLocal
        }

        // No conclusion reached, compare the sizes of the nodes then.
        return nodes.size.compareTo(other.nodes.size)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Lx

        if (nodes != other.nodes) return false

        return true
    }

    override fun hashCode(): Int {
        return nodes.hashCode()
    }

    override fun toString() =
        joinToString("/")
}

/**
 * Returns the common prefix of the receiver and [other].
 */
infix fun Lx.intersect(other: Lx) =
    // Zip nodes, take the receivers value until the pairs diverge, return as a new Lx.
    nodes.asSequence().zip(other.nodes.asSequence())
        .takeWhile { (a, b) -> a == b }
        .map { it.first }
        .toList()
        .let(::Lx)

/**
 * Removes from the receiver the common prefix in [other].
 */
infix fun Lx.subtract(other: Lx) =
    // Zip nodes, take the receivers value after the pairs diverge, return as a new Lx. Extend other by infinite units.
    nodes.asSequence().zip(other.nodes.asSequence().continuedBy { Unit })
        .dropWhile { (a, b) -> a == b }
        .map { it.first }
        .toList()
        .let(::Lx)

/**
 * Gets the immediate parent. Throws an [IllegalStateException] if no parent is present.
 */
val Lx.parent
    get() =
        validate(nodes.isNotEmpty()) { "Empty node has no parent" }
            ?: Lx(nodes.subList(0, nodes.size - 1))

/**
 * Returns the keys describing all children.
 */
val Lx.children
    get() = this / inf to this / sup

/**
 * Returns the keys describing all siblings. If this node is empty, returns [Inf] to [Sup].
 */
val Lx.siblings
    get() =
        if (nodes.isEmpty())
            inf to sup
        else
            parent.children

/**
 * Appends an exact coordinate.
 */
operator fun <T : Comparable<T>> Lx.div(value: T) =
    Lx(nodes + At(value))

/**
 * Appends a second lexicographic index.
 */
operator fun Lx.div(value: Lx) =
    Lx(nodes + value.nodes)

/**
 * Returns true if [child] is contained in the receiver, i.e., within `this / inf` and `this / sup`
 */
operator fun Lx.contains(child: Lx) =
    this / inf <= child && child <= this / sup

/**
 * An initial empty lexicographic index.
 */
val lx = Lx(emptyList())

/**
 * An initial [Inf] lexicographic index.
 */
val inf = Lx(listOf(Inf))

/**
 * An initial [Sup] lexicographic index.
 */
val sup = Lx(listOf(Sup))

/**
 * The last node value of the [Lx].
 */
fun Lx.lastValue() = last().requireValue()

/**
 * Repeats the entries of the [Lx].
 */
fun Lx.repeat(n: Int) = Lx(List(n * nodes.size) {
    nodes[it % nodes.size]
})