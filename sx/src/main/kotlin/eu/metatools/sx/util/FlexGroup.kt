package eu.metatools.sx.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.atomic.AtomicInteger

suspend inline fun <T, K, R> Flex<T>.groupBy(
    comparatorKey: Comparator<K>? = null,
    comparatorValue: Comparator<R>? = null,
    jobCount: Int = defaultJobCount,
    minParallel: Int = defaultMinParallel,
    scope: CoroutineScope = defaultPool,
    crossinline block: (value: T) -> Pair<K, R>
): NavigableMap<K, List<R>> {
    // Make iterator.
    val iterator = flexIterator()

    // Get actual comparator object. This is reused many times so it is pre-evaluated.
    @Suppress("unchecked_cast")
    val comparator = comparatorValue ?: compareBy { e: R -> e as Comparable<R> }

    // Check size condition.
    if (iterator.total < minParallel) {
        // Small amount of items, use standard iteration.
        val result = TreeMap<K, List<R>>(comparatorKey)

        // Iterate all, add result of block to map.
        repeat(iterator.total) {
            // Get association.
            val (k, new) = block(iterator.next())

            // Offer to result with insert.
            result.compute(k) { _, ex -> ex?.insert(new, comparator) ?: listOf(new) }
        }

        // Return map.
        return result
    } else {
        // Enough elements, use parallelism.
        val result = ConcurrentSkipListMap<K, List<R>>(comparatorKey)

        // Initialize remaining vector.
        val remaining = AtomicInteger(iterator.total)

        // Create all jobs.
        val jobs = (1..jobCount).map {
            scope.launch {
                while (0 < remaining.getAndDecrement()) {
                    // Get association.
                    val (k, new) = block(iterator.next())

                    // Offer to result with list append.
                    result.compute(k) { _, ex -> ex?.insert(new, comparator) ?: listOf(new) }
                }
            }
        }

        // Await all jobs.
        for (job in jobs)
            job.join()

        // Return map.
        return result
    }
}

suspend inline fun <T, K, R> Flex<T>.flatGroupBy(
    comparatorKey: Comparator<K>? = null,
    comparatorValue: Comparator<R>? = null,
    jobCount: Int = defaultJobCount,
    minParallel: Int = defaultMinParallel,
    scope: CoroutineScope = defaultPool,
    crossinline block: (value: T) -> Sequence<Pair<K, R>>
): NavigableMap<K, List<R>> {
    // Make iterator.
    val iterator = flexIterator()

    // Get actual comparator object. This is reused many times so it is pre-evaluated.
    @Suppress("unchecked_cast")
    val comparator = comparatorValue ?: compareBy { e: R -> e as Comparable<R> }

    // Check size condition.
    if (iterator.total < minParallel) {
        // Small amount of items, use standard iteration.
        val result = TreeMap<K, List<R>>(comparatorKey)

        // Iterate all, add result of block to map.
        repeat(iterator.total) {
            // Offer to result with merge.
            for ((k, new) in block(iterator.next()))
                result.compute(k) { _, ex -> ex?.insert(new, comparator) ?: listOf(new) }
        }

        // Return map.
        return result
    } else {
        // Enough elements, use parallelism.
        val result = ConcurrentSkipListMap<K, List<R>>(comparatorKey)

        // Initialize remaining vector.
        val remaining = AtomicInteger(iterator.total)

        // Create all jobs.
        val jobs = (1..jobCount).map {
            scope.launch {
                while (0 < remaining.getAndDecrement()) {
                    // Offer to result with merge.
                    for ((k, new) in block(iterator.next()))
                        result.compute(k) { _, ex -> ex?.insert(new, comparator) ?: listOf(new) }
                }
            }
        }

        // Await all jobs.
        for (job in jobs)
            job.join()

        // Return map.
        return result
    }
}