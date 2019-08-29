package eu.metatools.wep2.tools

import java.util.*

/**
 * Iterates a sequence per key.
 */
class ScopedSequence<K : Comparable<K>, I>(val sequence: Sequence<I>) {
    companion object {
        /**
         * Restores a [ScopedSequence] from the defining values of another scoped sequence.
         * @param sequence Static value of generating sequence.
         * @param scopes Defining value of currently active scopes.
         */
        fun <K : Comparable<K>, I> restore(
            sequence: Sequence<I>,
            scopes: Map<K, I>
        ): ScopedSequence<K, I> {
            // Create the result scoped sequence.
            val result = ScopedSequence<K, I>(sequence)

            // Take until semantically equal.
            scopes.forEach { (k, i) ->
                while (result.scopes[k] != i)
                    result.take(k)
            }

            // Return the newly created scoped sequence.
            return result
        }
    }

    /**
     * Currently active scopes.
     */
    private val scopeGenerators = TreeMap<K, Pair<Iterator<I>, I>>()

    /**
     * Gets the scope values mapping from scope key to last scope value.
     */
    val scopes get() = scopeGenerators.mapValues { (_, v) -> v.second }

    /**
     * Removes unused scopes up to but not including [key].
     */
    fun consolidate(key: K) =
        scopeGenerators
            .headMap(key, false)
            .clear()

    /**
     * Takes the next entry for the scope identified by [key].
     */
    fun take(key: K): I {
        // Get current scope value.
        val last = scopeGenerators[key]

        // Check if a scope was opened.
        if (last == null) {
            // Scope was not opened, create new.
            val iterator = sequence.iterator()
            val item = iterator.next()

            // Put the scope into the map and return the item.
            scopeGenerators[key] = iterator to item
            return item
        } else {
            // Scope was present, take next.
            val item = last.first.next()

            // Update scope in the map and return the item.
            scopeGenerators[key] = last.first to item
            return item
        }
    }
}