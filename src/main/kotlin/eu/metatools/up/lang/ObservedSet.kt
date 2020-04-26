package eu.metatools.up.lang

import eu.metatools.up.dt.Box
import java.util.*


/**
 * Delegates to a [NavigableSet] [actual] to implement the behavior. All adds and removes are tracked to [notify].
 * @property actual The actual implementation.
 * @property notify The listener.
 */
class ObservedSet<E>(
    val actual: NavigableSet<E>,
    val notify: (add: SortedSet<E>, remove: SortedSet<E>) -> Unit
) : NavigableSet<E> {
    override fun contains(element: E) =
        // Check if actual contains value.
        actual.contains(element)

    override fun lower(e: E): E? =
        // Get actual lower element.
        actual.lower(e)

    override fun floor(e: E): E? =
        // Get actual floor element.
        actual.floor(e)

    override fun higher(e: E): E? =
        // Get actual higher element.
        actual.higher(e)

    override fun add(element: E): Boolean {
        // Check if adding to actual changed the set.
        if (!actual.add(element))
            return false

        // Change occurred, notify and return true.
        notify(actual.aligned(element), actual.aligned())
        return true
    }

    override fun addAll(elements: Collection<E>): Boolean {
        // If empty, skip routine.
        if (elements.isEmpty())
            return false

        // Track adds and if mutated.
        val added = actual.aligned()

        // Iterate all elements, track if actually adding changed the set.
        for (element in elements)
            if (actual.add(element))
                added.add(element)

        // If no change occurred, return false.
        if (added.isEmpty())
            return false

        // Notify, return true.
        notify(added, actual.aligned())
        return true
    }

    override fun clear() {
        // Copy this set as the set of removed items, clear the actual set.
        val removed = TreeSet(actual)
        actual.clear()

        // If actually removed, notify.
        if (removed.isNotEmpty())
            notify(actual.aligned(), removed)
    }

    override fun tailSet(fromElement: E, inclusive: Boolean) =
        // Observe part of the set.
        ObservedSet(actual.tailSet(fromElement, inclusive), notify)

    override fun tailSet(fromElement: E): SortedSet<E> =
        // Observe part of the set.
        tailSet(fromElement, true)

    override fun remove(element: E): Boolean {
        // If removing did not change, return false.
        if (!actual.remove(element))
            return false

        // Notify removal and return true.
        notify(actual.aligned(), actual.aligned(element))
        return true
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        // Check if no elements are to be removed.
        if (elements.isEmpty())
            return false

        // Track removals.
        val removed = actual.aligned()
        actual.iterator().let {
            while (it.hasNext()) {
                val current = it.next()
                if (current in elements) {
                    it.remove()
                    removed.add(current)
                }
            }
        }

        // No removals, return unchanged.
        if (removed.isEmpty())
            return false

        // Notify changed and return true.
        notify(actual.aligned(), removed)
        return true
    }

    override fun iterator() =
        // Wrap iterator of actual  map.
        wrapIterator(actual.iterator())

    override fun ceiling(e: E): E? =
        // Get actual ceiling element.
        actual.ceiling(e)

    override fun first(): E =
        // Get actual first element.
        actual.first()

    override fun headSet(toElement: E, inclusive: Boolean) =
        // Observe part of the set.
        ObservedSet(actual.headSet(toElement, inclusive), notify)

    override fun headSet(toElement: E) =
        // Observe part of the set.
        headSet(toElement, false)

    override fun subSet(fromElement: E, fromInclusive: Boolean, toElement: E, toInclusive: Boolean) =
        // Observe part of the set.
        ObservedSet(actual.subSet(fromElement, fromInclusive, toElement, toInclusive), notify)

    override fun subSet(fromElement: E, toElement: E) =
        // Observe part of the set.
        subSet(fromElement, true, toElement, false)

    override fun retainAll(elements: Collection<E>): Boolean {
        // Check if no elements are to be retained.
        if (elements.isEmpty()) {
            // If already empty, return unchanged.
            if (isEmpty())
                return false

            // Clear set, return changed.
            clear()
            return true
        }

        // Track removals.
        val removed = actual.aligned()

        actual.iterator().let {
            while (it.hasNext()) {
                val current = it.next()
                if (current !in elements) {
                    it.remove()
                    removed.add(current)
                }
            }
        }

        // No removals, return unchanged.
        if (removed.isEmpty())
            return false

        // Notify changed and return true.
        notify(actual.aligned(), removed)
        return true
    }

    override val size: Int
        get() =
            // Delegate to actual size.
            actual.size

    override fun containsAll(elements: Collection<E>) =
        // Delegate to actual containment.
        actual.containsAll(elements)

    override fun isEmpty() =
        // Delegate to actual is empty.
        actual.isEmpty()

    override fun comparator(): Comparator<in E>? =
        // Return the comparator of the actual set.
        actual.comparator()

    override fun descendingSet() =
        // Observe the actual descending set.
        ObservedSet(actual.descendingSet(), notify)

    override fun pollFirst(): E? {
        // Poll the first entry.
        val result = actual.pollFirst()

        // If result was not null, notify.
        if (result != null)
            notify(actual.aligned(), actual.aligned(result))

        // Return the polled result.
        return result
    }

    override fun last(): E =
        // Return the actual last element.
        actual.last()

    override fun pollLast(): E? {
        // Poll the last entry.
        val result = actual.pollLast()

        // If result was not null, notify.
        if (result != null)
            notify(actual.aligned(), actual.aligned(result))

        // Return the polled result.
        return result
    }

    override fun descendingIterator() =
        // Wrap the actual descending iterator.
        wrapIterator(actual.descendingIterator())

    override fun hashCode() =
        actual.hashCode()

    override fun equals(other: Any?) =
        actual == other

    override fun toString() =
        actual.toString()

    /**
     * Wraps an iterator to generate notifications on proper changes.
     */
    private fun wrapIterator(iterator: MutableIterator<E>) = object : MutableIterator<E> {
        /**
         * The currently iterated element.
         */
        lateinit var current: Box<E>

        override fun hasNext() =
            // Has next if base also has next.
            iterator.hasNext()

        override fun next() =
            // Get base next value, remember it.
            iterator.next().also {
                current = Box(it)
            }

        override fun remove() {
            // Remove in base iterator.
            iterator.remove()

            // Notify removal.
            notify(actual.aligned(), actual.aligned(current.value))
        }
    }
}