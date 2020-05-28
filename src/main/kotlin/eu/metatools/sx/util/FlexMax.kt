package eu.metatools.sx.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Gets the maximum [Int] value as selected by [block]. Use for intense computations in [block].
 * @param jobCount The number of parallel jobs to use.
 * @param minParallel Minimum number of elements for parallel processing.
 * @param scope The coroutine scope.
 * @param block The mapping function.
 */
suspend inline fun <T> Flex<T>.max(
    jobCount: Int = defaultJobCount,
    minParallel: Int = defaultMinParallel,
    scope: CoroutineScope = defaultPool,
    crossinline block: (value: T) -> Int
): Int? {
    // Make iterator.
    val iterator = flexIterator()

    // Special case, also to allow for some initializations.
    if (iterator.total == 0)
        return null

    // Check size condition.
    if (iterator.total < minParallel) {
        // Small amount of items, use standard iteration.
        var result = block(iterator.next())

        // Iterate all, max by comparator.
        repeat(iterator.total.dec()) {
            // Exchange with max.
            result = maxOf(result, block(iterator.next()))
        }

        // Return value.
        return result
    } else {
        // Enough elements, use parallelism.
        val result = AtomicInteger(block(iterator.next()))

        // Initialize remaining vector.
        val remaining = AtomicInteger(iterator.total.dec())

        // Create all jobs.
        val jobs = (1..jobCount).map {
            scope.launch {
                // Max by comparator, exchange with max.
                while (0 < remaining.getAndDecrement())
                    result.getAndAccumulate(block(iterator.next()), ::maxOf)
            }
        }

        // Await all jobs.
        for (job in jobs)
            job.join()

        // Return value.
        return result.get()
    }
}

/**
 * Gets the maximum [Float] value as selected by [block]. Use for intense computations in [block].
 * @param jobCount The number of parallel jobs to use.
 * @param minParallel Minimum number of elements for parallel processing.
 * @param scope The coroutine scope.
 * @param block The mapping function.
 */
suspend inline fun <T> Flex<T>.maxFloat(
    jobCount: Int = defaultJobCount,
    minParallel: Int = defaultMinParallel,
    scope: CoroutineScope = defaultPool,
    crossinline block: (value: T) -> Float
): Float? {
    // Make iterator.
    val iterator = flexIterator()

    // Special case, also to allow for some initializations.
    if (iterator.total == 0)
        return null

    // Check size condition.
    if (iterator.total < minParallel) {
        // Small amount of items, use standard iteration.
        var result = block(iterator.next())

        // Iterate all, max by comparator.
        repeat(iterator.total.dec()) {
            // Exchange with max.
            result = maxOf(result, block(iterator.next()))
        }

        // Return value.
        return result
    } else {
        // Enough elements, use parallelism.
        val result = AtomicInteger(block(iterator.next()).toBits())

        // Initialize remaining vector.
        val remaining = AtomicInteger(iterator.total.dec())

        // Create all jobs.
        val jobs = (1..jobCount).map {
            scope.launch {
                // Max by comparator, exchange with max.
                while (0 < remaining.getAndDecrement())
                    result.getAndAccumulate(block(iterator.next()).toBits()) { a, b ->
                        maxOf(Float.fromBits(a), Float.fromBits(b)).toBits()
                    }
            }
        }

        // Await all jobs.
        for (job in jobs)
            job.join()

        // Return value.
        return Float.fromBits(result.get())
    }
}

/**
 * Gets the maximum element by [block]. Use for intense computations in [block].
 * @param jobCount The number of parallel jobs to use.
 * @param minParallel Minimum number of elements for parallel processing.
 * @param scope The coroutine scope.
 * @param block The mapping function.
 */
suspend inline fun <T> Flex<T>.maxBy(
    jobCount: Int = defaultJobCount,
    minParallel: Int = defaultMinParallel,
    scope: CoroutineScope = defaultPool,
    crossinline block: (value: T) -> Int
): T? {
    // Make iterator.
    val iterator = flexIterator()

    // Special case, also to allow for some initializations.
    if (iterator.total == 0)
        return null

    // Check size condition.
    if (iterator.total < minParallel) {
        // Small amount of items, use standard iteration.
        var result = iterator.next().let { it to block(it) }

        // Iterate all, max by comparator.
        repeat(iterator.total.dec()) {
            // Get value and selector.
            val new = iterator.next().let { it to block(it) }

            // Exchange for greater value.
            if (result.second < new.second)
                result = new
        }

        // Return value.
        return result.first
    } else {
        // Enough elements, use parallelism.
        val result = AtomicReference(iterator.next().let { it to block(it) })

        // Initialize remaining vector.
        val remaining = AtomicInteger(iterator.total.dec())

        // Create all jobs.
        val jobs = (1..jobCount).map {
            scope.launch {
                // Max by comparator.
                while (0 < remaining.getAndDecrement()) {
                    // Get value and selector.
                    val new = iterator.next().let { it to block(it) }

                    // Exchange if result not assigned or assigned to a smaller element.
                    result.getAndAccumulate(new) { a, b ->
                        if (a.second < b.second) b else a
                    }
                }
            }
        }

        // Await all jobs.
        for (job in jobs)
            job.join()

        // Return value.
        return result.get()?.first
    }
}

/**
 * Gets the maximum element by [block]. Use for intense computations in [block].
 * @param jobCount The number of parallel jobs to use.
 * @param minParallel Minimum number of elements for parallel processing.
 * @param scope The coroutine scope.
 * @param block The mapping function.
 */
suspend inline fun <T> Flex<T>.maxByFloat(
    jobCount: Int = defaultJobCount,
    minParallel: Int = defaultMinParallel,
    scope: CoroutineScope = defaultPool,
    crossinline block: (value: T) -> Float
): T? {
    // Make iterator.
    val iterator = flexIterator()

    // Special case, also to allow for some initializations.
    if (iterator.total == 0)
        return null

    // Check size condition.
    if (iterator.total < minParallel) {
        // Small amount of items, use standard iteration.
        var result = iterator.next().let { it to block(it) }

        // Iterate all, max by comparator.
        repeat(iterator.total.dec()) {
            // Get value and selector.
            val new = iterator.next().let { it to block(it) }

            // Exchange for greater value.
            if (result.second < new.second)
                result = new
        }

        // Return value.
        return result.first
    } else {
        // Enough elements, use parallelism.
        val result = AtomicReference(iterator.next().let { it to block(it) })

        // Initialize remaining vector.
        val remaining = AtomicInteger(iterator.total.dec())

        // Create all jobs.
        val jobs = (1..jobCount).map {
            scope.launch {
                // Max by comparator.
                while (0 < remaining.getAndDecrement()) {
                    // Get value and selector.
                    val new = iterator.next().let { it to block(it) }

                    // Exchange if result not assigned or assigned to a smaller element.
                    result.getAndAccumulate(new) { a, b ->
                        if (a.second < b.second) b else a
                    }
                }
            }
        }

        // Await all jobs.
        for (job in jobs)
            job.join()

        // Return value.
        return result.get()?.first
    }
}