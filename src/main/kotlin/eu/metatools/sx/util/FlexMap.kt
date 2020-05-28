package eu.metatools.sx.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicInteger

/**
 * Maps the collection in parallel. [R] should be comparable or a [comparator] should be given.
 * @param comparator The comparator of the results.
 * @param jobCount The number of parallel jobs to use.
 * @param minParallel Minimum number of elements for parallel processing.
 * @param scope The coroutine scope.
 * @param block The mapping function.
 */
suspend inline fun <T, R> Flex<T>.mapDistinct(
    comparator: Comparator<R>? = null,
    jobCount: Int = defaultJobCount,
    minParallel: Int = defaultMinParallel,
    scope: CoroutineScope = defaultPool,
    crossinline block: (value: T) -> R
): NavigableSet<R> {
    // Make iterator.
    val iterator = flexIterator()

    // Check size condition.
    if (iterator.total < minParallel) {
        // Small amount of items, use standard iteration.
        val result = TreeSet(comparator)

        // Iterate all, add result of block to set.
        repeat(iterator.total) {
            result.add(block(iterator.next()))
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
                // Add mapped items.
                while (0 < remaining.getAndDecrement())
                    result.add(block(iterator.next()))
            }
        }

        // Await all jobs.
        for (job in jobs)
            job.join()

        // Return set.
        return result
    }
}

/**
 * Flat maps the collection in parallel. [R] should be comparable or a [comparator] should be given.
 * @param comparator The comparator of the results.
 * @param jobCount The number of parallel jobs to use.
 * @param minParallel Minimum number of elements for parallel processing.
 * @param scope The coroutine scope.
 * @param block The mapping function.
 */
suspend inline fun <T, R> Flex<T>.flatMapDistinct(
    comparator: Comparator<R>? = null,
    jobCount: Int = defaultJobCount,
    minParallel: Int = defaultMinParallel,
    scope: CoroutineScope = defaultPool,
    crossinline block: (value: T) -> Sequence<R>
): NavigableSet<R> {
    // Make iterator.
    val iterator = flexIterator()

    // Check size condition.
    if (iterator.total < minParallel) {
        // Small amount of items, use standard iteration.
        val result = TreeSet(comparator)

        // Iterate all, add result of block to set.
        repeat(iterator.total) {
            result.addAll(block(iterator.next()))
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
                // Add mapped items.
                while (0 < remaining.getAndDecrement())
                    result.addAll(block(iterator.next()))
            }
        }

        // Await all jobs.
        for (job in jobs)
            job.join()

        // Return set.
        return result
    }
}