package eu.metatools.sx.util

import kotlinx.coroutines.runBlocking
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.system.measureTimeMillis

/**
 * Size-delimited iterable.
 */
interface Flex<T> {
    /**
     * Creates a new flex iterator.
     */
    fun flexIterator(): FlexIterator<T>
}

/**
 * Size-delimited iterator.
 */
interface FlexIterator<T> {
    /**
     * Total number of elements in the iteration.
     */
    val total: Int

    /**
     * Gets the next value.
     */
    fun next(): T
}

/**
 * Converts the [Flex] to a [Sequence].
 */
fun <T> Flex<T>.asSequence() = sequence {
    // Get source iterator.
    val iterator = flexIterator()

    // Yield all.
    repeat(iterator.total) {
        yield(iterator.next())
    }
}

/**
 * Returns an index iterable on the list.
 */
fun <E> List<E>.flex() = object : Flex<E> {
    override fun flexIterator() =
        object : FlexIterator<E> {
            override val total: Int = size

            /**
             * Current location.
             */
            private val at = AtomicInteger(0)

            override fun next() =
                // Get the value at the current location and increment.
                get(at.getAndIncrement())
        }
}

/**
 * Converts the receiver to a list and returns an [Flex] on that.
 */
fun <E> Set<E>.flex() =
    toList().flex()

/**
 * Converts the receiver to a list and returns an [Flex] on that.
 */
fun <K, V> Map<K, V>.flex() =
    toList().flex()


/**
 * Returns an index iterable on the navigable set.
 */
fun <E> NavigableSet<E>.flex() = object : Flex<E> {
    override fun flexIterator() = object : FlexIterator<E> {
        override val total = size

        val at = AtomicReference(first())

        override fun next() =
            at.getAndUpdate {
                higher(it)
            }
    }
}

/**
 * Returns an index iterable on the navigable map.
 */
fun <K, V> NavigableMap<K, V>.flex() = object : Flex<Pair<K, V>> {
    override fun flexIterator() = object : FlexIterator<Pair<K, V>> {
        override val total = size

        val at = AtomicReference(firstEntry())

        override fun next() =
            at.getAndUpdate {
                higherEntry(it.key)
            }.toPair()
    }
}

fun main() = runBlocking {
    fun Int.prime() = (2..(this / 2)).none {
        this % it == 0
    }

    val source = (0..100000).toList()
    measureTimeMillis {
        val resultA = source.filter { it.prime() }
        println(resultA)
    }.let(::println)

    measureTimeMillis {
        val resultB = source.flex().filter { it.prime() }
        println(resultB)
    }.let(::println)
}

//fun main() = runBlocking {
//    // Definition of work.
//    fun work(value: Pair<Int, String>): Map<Int, Int> {
//        fun Int.divisors() = (2..(this / 2)).count {
//            this % it == 0
//        }
//
//        val cat = value.first.divisors()
//        return mapOf(cat to 1, -cat to 1)
//    }
//
//
//    // Num iterations.
//    val iterations = 5
//
//    // Source data.
//    val sourceData = (0..20000).associateWithTo(TreeMap()) { it.toString(16) }
//
//    // Iterate different number of jobs.
//    (1..8).forEach { exp ->
//        // Compute job amount.
//        val jobCount = 2.0.pow(exp).roundToInt()
//
//        println("$jobCount jobs:")
//
//        // Aggregate times.
//        var seq = 0L
//        var par = 0L
//
//        // Do sequential.
//        repeat(iterations) {
//            seq += measureNanoTime {
//                sourceData.flex().flatMergeAssociate(Int.add, null, 1, Int.MAX_VALUE,
//                    defaultPool, ::work)
//                print(".")
//            }
//        }
//
//        // Print stats.
//        println()
//        println(seq / iterations)
//
//        // Do parallel.
//        repeat(iterations) {
//            par += measureNanoTime {
//                sourceData.flex().flatMergeAssociate(Int::plus, null, jobCount, 1,
//                    defaultPool, ::work)
//                print(".")
//            }
//        }
//
//        // Print stats.
//        println()
//        println(par / iterations)
//
//        // Print factor.
//        println("${(seq.toFloat() / par).let { (10f * it).roundToInt() / 10f }} times faster")
//    }
//}