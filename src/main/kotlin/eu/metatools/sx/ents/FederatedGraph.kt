package eu.metatools.sx.ents

import java.util.*

class FederatedGraph<N : Comparable<N>>(
    val forward: () -> NavigableMap<N, Set<N>>,
    val backward: () -> NavigableMap<N, Set<N>>
) : Iterable<Pair<N, N>> {
    /**
     * Add connection [from] node [to] node.
     */
    fun add(from: N, to: N) {
        forward().compute(from) { _, e ->
            if (e == null) setOf(to) else e + to
        }
        backward().compute(to) { _, e ->
            if (e == null) setOf(from) else e + from
        }
    }


    /**
     * Removes the edge [from] node [to] node.
     */
    fun remove(from: N, to: N) {
        forward().computeIfPresent(from) { _, set ->
            (set - to).takeIf { it.isNotEmpty() }
        }
        backward().computeIfPresent(to) { _, set ->
            (set - from).takeIf { it.isNotEmpty() }
        }
    }

    /**
     * Removes all edges from and to [node].
     */
    fun remove(node: N) {
        for (to in from(node))
            remove(node, to)
        for (from in to(node))
            remove(from, node)
    }

    /**
     * All nodes reachable from [node].
     */
    fun from(node: N) =
        forward()[node].orEmpty().toSortedSet()

    /**
     * All nodes reaching to [node].
     */
    fun to(node: N) =
        backward()[node].orEmpty().toSortedSet()

    /**
     * Amount of nodes reachable from [node].
     */
    fun fromSize(node: N) =
        forward()[node]?.size ?: 0

    /**
     * Amount of nodes reaching to [node].
     */
    fun toSize(node: N) =
        backward()[node]?.size ?: 0

    /**
     * Clears the graph.
     */
    fun clear() {
        forward().clear()
        backward().clear()
    }

    override fun iterator() =
        // Use forward set.
        forward()
            // Get all entries.
            .entries
            // Use sequence representation.
            .asSequence()
            // Associate key node to each value node.
            .flatMap { (k, vs) -> vs.asSequence().map { v -> k to v } }
            // Iterate.
            .iterator()
}