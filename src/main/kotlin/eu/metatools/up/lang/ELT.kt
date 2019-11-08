package eu.metatools.up.lang

import kotlin.reflect.full.companionObjectInstance

/**
 * The base of the structure, providing the zero element.
 */
interface Zero<T> : () -> T

/**
 * Returns the zero element for an [A] properly implementing a companion of type [Zero].
 */
inline fun <reified T : A<T>> zero(): T {
    // Get companion instance, invoke constructor and return it if not zero.
    val instance = T::class.companionObjectInstance as? Zero<*>
    val value = instance?.invoke() as? T
    if (value != null)
        return value

    // Was zero, throw an exception.
    error("Companion object of ${T::class.simpleName} must implement Zero for the instance type.")
}

/**
 * An abelian structure. Supports inversion via [unaryMinus] and an operation [op].
 */
interface A<T : A<T>> {
    /**
     * Returns the production of zero.
     */
    val zero: Zero<T>

    /**
     * Inverts the object.
     */
    operator fun unaryMinus(): T

    /**
     * Applies the binary operation.
     */
    infix fun op(right: T): T

    /**
     * True if this value is zero, defaults to checking if equal to the value of [zero].
     */
    val isZero
        get() = equals(zero())
}

/**
 * True if the value is not zero.
 */
val A<*>.isNotZero
    get() = !isZero

/**
 * Nullable merge variant. Assumes the structures zero if not present.
 */
inline infix fun <reified T : A<T>> T?.op(right: T?) =
    if (this == null) {
        right ?: zero()
    } else {
        if (right == null)
            this
        else
            op(right)
    }

/**
 * Merges two values without explicit type hints. Asserts that they are can be merged by checking equivalence on the
 * structures they conform to.
 */
infix fun A<*>.checkedOp(right: A<*>): A<*> {
    if (zero == right.zero)
        @Suppress("member_projected_out")
        return op(right)

    error("Structures $zero and ${right.zero} are not compatible.")
}

/**
 * A natural number that is also an [A].
 * @property value The value of the number.
 */
data class Nat(val value: Int) : A<Nat>, Comparable<Nat> {
    companion object : Zero<Nat> {
        override fun invoke() =
            Nat(0)
    }

    override val zero
        get() = Nat

    override fun unaryMinus() =
        Nat(-value)

    override fun op(right: Nat) =
        Nat(value + right.value)

    override val isZero
        get() = value == 0

    override fun toString() =
        value.toString()

    override fun compareTo(other: Nat) =
        value.compareTo(other.value)
}

/**
 * Overload creating an [A] for ints.
 */
@JvmName("aNat")
fun a(value: Int) =
    Nat(value)

/**
 * Multiplies the receiver [n] times.
 */
inline operator fun <reified T : A<T>> T.times(n: Int): T {
    var result = zero<T>()
    repeat(n) { result = result op this }
    return result
}

/**
 * A key/value map. Implements [A].
 * @param assignments The assignments to use. Assignments to values that are [A.isZero] are omitted.
 * @property assignments The assignments of this key/value map.
 */
@Suppress("DataClassPrivateConstructor")
class KV<K, V : A<V>>(assignments: Map<K, V>) :
    A<KV<K, V>> {
    /**
     * The assignments of this key/value map.
     */
    val assignments = assignments.filterValues { it.isNotZero }

    companion object : Zero<KV<*, *>> {
        override fun invoke() = KV<Nothing, Nothing>(mapOf())
    }

    override val zero: Zero<KV<K, V>>
        @Suppress("unchecked_cast")
        get() = KV as Zero<KV<K, V>>

    override fun unaryMinus() =
        KV(assignments.mapValues { (_, v) -> -v })

    override fun op(right: KV<K, V>): KV<K, V> {
        // Copy local map.
        val new = assignments.toMutableMap()

        // For all assignments in right map.
        for ((k, v) in right.assignments)
            new.compute(k) { _, present ->
                // Merge values if key was present, otherwise assign. Remove if it is zero.
                (present?.op(v) ?: v).takeIf { it.isNotZero }
            }

        // Return map with the new assignments.
        return KV(new)
    }

    override val isZero
        get() = assignments.isEmpty()


    override fun toString() =
        assignments.toString()

    override fun equals(other: Any?) =
        this === other || other is KV<*, *> && checkedOp(-other).isZero

    override fun hashCode(): Int =
        assignments.hashCode()
}

/**
 * Overload creating an [A] for arbitrary maps.
 */
@JvmName("aKV")
fun <K, V : A<V>> a(assignments: Map<K, V>) =
    KV(assignments)

/**
 * Alias of a map from key to natural number.
 */
typealias Bag<K> = KV<K, Nat>

/**
 * Overload creating an [A] for bags.
 */
@JvmName("aBag")
fun <K> a(assignments: Map<K, Int>) =
    Bag(assignments.mapValues { (_, v) -> a(v) })


fun main() {
    val x = a(
        mapOf(
            "a" to 1,
            "b" to 2,
            "c" to 3
        )
    )

    val y = a(
        mapOf(
            "a" to 1,
            "b" to 2,
            "c" to 3
        )
    )

    val z = a(
        mapOf(
            "a" to 4,
            "b" to 3,
            "c" to 4
        )
    )

    println(x == y)
}