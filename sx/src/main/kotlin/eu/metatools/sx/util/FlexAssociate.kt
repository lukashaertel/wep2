package eu.metatools.sx.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Associates the collection in parallel. Merges equal key results. [R] should be comparable or a [comparator] should be
 * given.
 * @param comparator The comparator of the results.
 * @param jobCount The number of parallel jobs to use.
 * @param minParallel Minimum number of elements for parallel processing.
 * @param scope The coroutine scope.
 * @param merge The merge operation if keys are equal.
 * @param block The mapping function.
 */
suspend inline fun <T, K, R> Flex<T>.associate(
    crossinline merge: (R, R) -> R,
    comparator: Comparator<K>? = null,
    jobCount: Int = defaultJobCount,
    minParallel: Int = defaultMinParallel,
    scope: CoroutineScope = defaultPool,
    crossinline block: (value: T) -> Pair<K, R>
): NavigableMap<K, R> {
    // Make iterator.
    val iterator = flexIterator()

    // Check size condition.
    if (iterator.total < minParallel) {
        // Small amount of items, use standard iteration.
        val result = TreeMap<K, R>(comparator)

        // Iterate all, add result of block to map.
        repeat(iterator.total) {
            // Get association.
            val (k, new) = block(iterator.next())

            // Offer to result with merge.
            result.compute(k) { _, ex -> if (ex == null) new else merge(ex, new) }
        }

        // Return map.
        return result
    } else {
        // Enough elements, use parallelism.
        val result = ConcurrentSkipListMap<K, R>(comparator)

        // Initialize remaining vector.
        val remaining = AtomicInteger(iterator.total)

        // Create all jobs.
        val jobs = (1..jobCount).map {
            scope.launch {
                while (0 < remaining.getAndDecrement()) {
                    // Get association.
                    val (k, new) = block(iterator.next())

                    // Offer to result with merge.
                    result.compute(k) { _, ex -> if (ex == null) new else merge(ex, new) }
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

/**
 * Flat associates the collection in parallel. Merges equal key results. [R] should be comparable or a [comparator]
 * should be given.
 * @param comparator The comparator of the results.
 * @param jobCount The number of parallel jobs to use.
 * @param minParallel Minimum number of elements for parallel processing.
 * @param scope The coroutine scope.
 * @param merge The merge operation if keys are equal.
 * @param block The mapping function.
 */
suspend inline fun <T, K, R> Flex<T>.flatAssociate(
    crossinline merge: (R, R) -> R,
    comparator: Comparator<K>? = null,
    jobCount: Int = defaultJobCount,
    minParallel: Int = defaultMinParallel,
    scope: CoroutineScope = defaultPool,
    crossinline block: (value: T) -> Sequence<Pair<K, R>>
): NavigableMap<K, R> {
    // Make iterator.
    val iterator = flexIterator()

    // Check size condition.
    if (iterator.total < minParallel) {
        // Small amount of items, use standard iteration.
        val result = TreeMap<K, R>(comparator)

        // Iterate all, add result of block to map.
        repeat(iterator.total) {
            // Offer to result with merge.
            for ((k, new) in block(iterator.next()))
                result.compute(k) { _, ex -> if (ex == null) new else merge(ex, new) }
        }

        // Return map.
        return result
    } else {
        // Enough elements, use parallelism.
        val result = ConcurrentSkipListMap<K, R>(comparator)

        // Initialize remaining vector.
        val remaining = AtomicInteger(iterator.total)

        // Create all jobs.
        val jobs = (1..jobCount).map {
            scope.launch {
                while (0 < remaining.getAndDecrement()) {
                    // Offer to result with merge.
                    for ((k, new) in block(iterator.next()))
                        result.compute(k) { _, ex -> if (ex == null) new else merge(ex, new) }
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

/**
 * Associates the collection in parallel. Does not support merging on equal key assignments. [R] should be comparable or
 * a [comparator] should be given.
 * @param comparator The comparator of the results.
 * @param jobCount The number of parallel jobs to use.
 * @param minParallel Minimum number of elements for parallel processing.
 * @param scope The coroutine scope.
 * @param block The mapping function.
 */
suspend inline fun <T, K, R> Flex<T>.associate(
    comparator: Comparator<K>? = null,
    jobCount: Int = defaultJobCount,
    minParallel: Int = defaultMinParallel,
    scope: CoroutineScope = defaultPool,
    crossinline block: (value: T) -> Pair<K, R>
) = associate(
    { a, b -> throw UnsupportedOperationException("Cannot merge $a and $b.") },
    comparator, jobCount, minParallel, scope, block
)

/**
 * Flat associates the collection in parallel. Does not support merging on equal key assignments. [R] should be
 * comparable or a [comparator] should be given.
 * @param comparator The comparator of the results.
 * @param jobCount The number of parallel jobs to use.
 * @param minParallel Minimum number of elements for parallel processing.
 * @param scope The coroutine scope.
 * @param block The mapping function.
 */
suspend inline fun <T, K, R> Flex<T>.flatAssociate(
    comparator: Comparator<K>? = null,
    jobCount: Int = defaultJobCount,
    minParallel: Int = defaultMinParallel,
    scope: CoroutineScope = defaultPool,
    crossinline block: (value: T) -> Sequence<Pair<K, R>>
) = flatAssociate(
    { a, b -> throw UnsupportedOperationException("Cannot merge $a and $b.") },
    comparator, jobCount, minParallel, scope, block
)
