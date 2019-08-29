package eu.metatools.mk.tools

import java.util.*

/**
 * Iterates a sequence per key.
 */
class ScopedSequence<K : Comparable<K>, I>(val sequence: Sequence<I>) {
    /**
     * Currently active scopes.
     */
    private val scopes = TreeMap<K, Iterator<I>>()

    /**
     * Removes unused scopes up to but not including [key].
     */
    fun consolidate(key: K) =
        scopes
            .headMap(key, false)
            .clear()

    /**
     * Takes the next entry for the scope identified by [key].
     */
    fun take(key: K) =
        scopes.getOrPut(key, sequence::iterator).next()
}