package eu.metatools.sx.process

import eu.metatools.fio.data.Tri
import eu.metatools.sx.data.Volume
import java.util.*
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * A volume transformation process.
 */
abstract class Process<V : Any, R : Any> {
    /**
     * Computes an update map for the given volume.
     */
    abstract fun compute(volume: Volume<V>): SortedMap<Tri, R>
}

/**
 * A volume transformation process running with an 8-parallelism.
 * @property executor The executor to run. Defaults to a fixed thread pool of eight threads.
 * @property minSize The minimum parallel required size, under this, no parallel execution is performed.
 * @property putBatch The amount of items to put in a single batch from a task.
 */
abstract class ProcessParallel<V : Any, R : Any>(
    val executor: ExecutorService = DEFAULT_EXECUTOR,
    val minSize: Int = DEFAULT_MIN_SIZE,
    val putBatch: Int = DEFAULT_PUT_BATCH
) : Process<V, R>() {
    companion object {
        /**
         * The default executor, a fixed thread pool executor that spawns daemon threads.
         */
        val DEFAULT_EXECUTOR: ExecutorService = Executors.newFixedThreadPool(8) {
            // Use the default factory, make the resulting thread a daemon thread.
            Executors.defaultThreadFactory().newThread(it).apply {
                isDaemon = true
            }
        }

        /**
         * Default minimum parallel required size.
         */
        const val DEFAULT_MIN_SIZE = 64

        /**
         * Default put batch size.
         */
        const val DEFAULT_PUT_BATCH = 32

        /**
         * Shorthand for range.
         */
        @Suppress("nothing_to_inline")
        private inline fun upTo(value: Int) = Int.MIN_VALUE until value

        /**
         * Shorthand for range.
         */
        @Suppress("nothing_to_inline")
        private inline fun startingAt(value: Int) = value..Int.MAX_VALUE
    }

    /**
     * Performs the computation on the sub-range.
     */
    abstract fun computeSegment(volume: Volume<V>, xs: IntRange, ys: IntRange, zs: IntRange): Sequence<Pair<Tri, R>>

    /**
     * Merges two assignments, by default not supported.
     */
    protected open fun merge(first: R, second: R): R =
        throw UnsupportedOperationException("No merge operator defined.")

    override fun compute(volume: Volume<V>): SortedMap<Tri, R> {
        // Make result map.
        val result = ConcurrentSkipListMap<Tri, R>()

        // Get center for splitting.
        val center = volume.findCenter()

        fun body(xs: IntRange, ys: IntRange, zs: IntRange) =
            // Compute updates. Add to map in chunks via merge if necessary.
            computeSegment(volume, xs, ys, zs)
                .chunked(putBatch)
                .forEach { updates ->
                    for ((k, new) in updates) {
                        result.compute(k) { _, existing ->
                            if (existing == null) new else merge(existing, new)
                        }
                    }
                }

        /**
         * Submits a tasks for the given range.
         */
        fun task(xs: IntRange, ys: IntRange, zs: IntRange) = executor.submit {
            body(xs, ys, zs)
        }

        // Check if minimum size for parallelism is given.
        if (volume.size < minSize) {
            // Run directly.
            body(Int.MIN_VALUE..Int.MAX_VALUE, Int.MIN_VALUE..Int.MAX_VALUE, Int.MIN_VALUE..Int.MAX_VALUE)
        } else {
            // Start all tasks.
            val t0 = task(upTo(center.x), upTo(center.y), upTo(center.z))
            val t1 = task(upTo(center.x), upTo(center.y), startingAt(center.z))
            val t2 = task(upTo(center.x), startingAt(center.y), upTo(center.z))
            val t3 = task(upTo(center.x), startingAt(center.y), startingAt(center.z))
            val t4 = task(startingAt(center.x), upTo(center.y), upTo(center.z))
            val t5 = task(startingAt(center.x), upTo(center.y), startingAt(center.z))
            val t6 = task(startingAt(center.x), startingAt(center.y), upTo(center.z))
            val t7 = task(startingAt(center.x), startingAt(center.y), startingAt(center.z))

            // Wait all tasks.
            t0.get()
            t1.get()
            t2.get()
            t3.get()
            t4.get()
            t5.get()
            t6.get()
            t7.get()
        }

        // Return result, unbox self association.
        return result
    }
}

/**
 * Processes point-wise.
 */
abstract class ProcessAt<V : Any, R : Any>(
    executor: ExecutorService = DEFAULT_EXECUTOR,
    minSize: Int = DEFAULT_MIN_SIZE,
    putBatch: Int = DEFAULT_PUT_BATCH
) : ProcessParallel<V, R>(executor, minSize, putBatch) {
    /**
     * Computes the sub-segment for a single point.
     */
    protected abstract fun computeAt(volume: Volume<V>, x: Int, y: Int, z: Int, value: V): Sequence<Pair<Tri, R>>

    override fun computeSegment(volume: Volume<V>, xs: IntRange, ys: IntRange, zs: IntRange) =
        // Get sequence of values in source.
        volume[xs, ys, zs].flatMap { (at, value) ->
            // Compute new value and associate.
            computeAt(volume, at.x, at.y, at.z, value)
        }
}

