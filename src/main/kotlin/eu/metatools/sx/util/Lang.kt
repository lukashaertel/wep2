package eu.metatools.sx.util

import java.util.*

/**
 * Makes a new list with the element inserted with the given sorting.
 */
fun <E> List<E>.insert(element: E, comparator: Comparator<E>): List<E> {
    val index = binarySearch(element, comparator)
    if (index >= 0)
        return this

    val ip = (-index - 1)
    return ArrayList<E>().also {
        it.addAll(subList(0, ip))
        it.add(element)
        it.addAll(subList(ip, size))
    }
}

/**
 * Makes a new list with the element inserted with the default sorting.
 */
fun <E : Comparable<E>> List<E>.insert(element: E): List<E> {
    val index = binarySearch(element)
    if (index >= 0)
        return this

    val ip = (-index - 1)
    return ArrayList<E>().also {
        it.addAll(subList(0, ip))
        it.add(element)
        it.addAll(subList(ip, size))
    }
}

/**
 * Breadth first searches an [N] satisfying [condition] starting at [start] and expanding of ver [next]. If
 * nothing was found, `null` is returned. Returns the entire path from [start] to an [N] that satisfies [condition].
 */
inline fun <N> findFirstPath(start: N, condition: (N) -> Boolean, next: (N) -> Sequence<N>): List<N>? {
    // Front of paths and seen vertices.
    val front = LinkedList<List<N>>().also { it.add(listOf(start)) }
    val seen = hashSetOf<N>()

    // Repeat until empty or something was found.
    while (true) {
        // Get first path, if nothing, return null.
        val current = front.pollFirst()
            ?: return null

        // Get end node.
        val end = current.last()

        // If end node satisfies condition, return it.
        if (condition(end))
            return current

        // Offer to seen so it is not visited twice.
        seen.add(end)

        // Append next elements that have not been visited yet.
        for (out in next(end))
            if (out !in seen)
                front.add(current + out)
    }
}

/**
 * Breadth first searches an [N] satisfying [condition] starting at [start] and expanding of ver [next]. If
 * nothing was found, `null` is returned.
 */
inline fun <N> findFirst(start: N, condition: (N) -> Boolean, next: (N) -> Sequence<N>): N? {
    // Front and seen vertices.
    val front = LinkedList<N>().also { it.add(start) }
    val seen = hashSetOf<N>()

    // Repeat until empty or something was found.
    while (true) {
        // Get first entry, if nothing, return null.
        val current = front.pollFirst()
            ?: return null

        // If entry satisfies condition, return it.
        if (condition(current))
            return current

        // Offer to seen so it is not visited twice.
        seen.add(current)

        // Add next elements that have not been visited yet.
        for (out in next(current))
            if (out !in seen)
                front.add(out)
    }
}