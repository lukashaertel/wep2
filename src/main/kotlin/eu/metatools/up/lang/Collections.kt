package eu.metatools.up.lang

import java.util.*


/**
 * Retains sorting of the receiver for a new navigable set.
 */
fun <E> SortedSet<E>.aligned(): NavigableSet<E> =
    TreeSet(comparator())

/**
 * Retains sorting of the receiver for a new navigable set with the given [elements].
 */
fun <E> SortedSet<E>.aligned(elements: Collection<E>): NavigableSet<E> =
    TreeSet(comparator()).also { it.addAll(elements) }

/**
 * Retains sorting of the receiver for a new navigable set with the given [elements].
 */
fun <E> SortedSet<E>.aligned(vararg elements: E): NavigableSet<E> =
    TreeSet(comparator()).also { it.addAll(elements) }

/**
 * Retains sorting of the receiver for a new navigable set with the given [elements].
 */
fun <E> SortedSet<E>.aligned(elements: Iterable<E>): NavigableSet<E> =
    TreeSet(comparator()).also { it.addAll(elements) }

/**
 * Retains sorting of the receiver for a new navigable set with the given [elements].
 */
fun <E> SortedSet<E>.aligned(elements: Sequence<E>): NavigableSet<E> =
    TreeSet(comparator()).also { it.addAll(elements) }


/**
 * Retains sorting of the receiver for a new navigable map.
 */
fun <K, V> SortedMap<K, V>.aligned(): NavigableMap<K, V> =
    TreeMap(comparator())

/**
 * Retains sorting of the receiver for a new navigable map copied [from] the source.
 */
fun <K, V> SortedMap<K, V>.aligned(from: Map<K, V>): NavigableMap<K, V> =
    TreeMap<K, V>(comparator()).also { it.putAll(from) }

/**
 * Retains sorting of the receiver for a new navigable map with the given [entries].
 */
fun <K, V> SortedMap<K, V>.aligned(vararg entries: Pair<K, V>): NavigableMap<K, V> =
    TreeMap<K, V>(comparator()).also { it.putAll(entries) }

/**
 * Retains sorting of the receiver for a new navigable map with the given [entries].
 */
fun <K, V> SortedMap<K, V>.aligned(entries: Iterable<Pair<K, V>>): NavigableMap<K, V> =
    TreeMap<K, V>(comparator()).also { it.putAll(entries) }

/**
 * Retains sorting of the receiver for a new navigable map with the given [entries].
 */
fun <K, V> SortedMap<K, V>.aligned(entries: Sequence<Pair<K, V>>): NavigableMap<K, V> =
    TreeMap<K, V>(comparator()).also { it.putAll(entries) }

/**
 * Gets the sub-list excluding the last element.
 */
fun <E> List<E>.init() = subList(0, size - 1)

/**
 * Gets the sub-list excluding the first element.
 */
fun <E> List<E>.tail() = subList(1, size)