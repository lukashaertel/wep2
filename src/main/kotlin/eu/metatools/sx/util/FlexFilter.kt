package eu.metatools.sx.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicInteger

/**
 * Filters the collection in parallel. [T] should be comparable or a [comparator] should be given.
 * @param comparator The comparator of the results.
 * @param jobCount The number of parallel jobs to use.
 * @param minParallel Minimum number of elements for parallel processing.
 * @param scope The coroutine scope.
 * @param block The filtering function.
 */
suspend inline fun <T> Flex<T>.filter(
    comparator: Comparator<T>? = null,
    jobCount: Int = defaultJobCount,
    minParallel: Int = defaultMinParallel,
    scope: CoroutineScope = defaultPool,
    crossinline block: (value: T) -> Boolean
): NavigableSet<T> {
    // Make iterator.
    val iterator = flexIterator()

    // Check size condition.
    if (iterator.total < minParallel) {
        // Small amount of items, use standard iteration.
        val result = TreeSet(comparator)

        // Iterate all, add to set if filter returned true.
        repeat(iterator.total) {
            val value = iterator.next()
            if (block(value))
                result.add(value)
        }

        // Return set.
        return result
    } else {
        // Enough elements, use parallelism.
        val result = ConcurrentSkipListSet(comparator)

        // Initialize remaining vector.
        val remaining = AtomicInteger(iterator.total)

        // Create all jobs.
        val jobs = (1..jobCount).map {
            scope.launch {
                // Add filtered items.
                while (0 < remaining.getAndDecrement()) {
                    val value = iterator.next()
                    if (block(value))
                        result.add(value)
                }
            }
        }

        // Await all jobs.
        for (job in jobs)
            job.join()

        // Return set.
        return result
    }
}