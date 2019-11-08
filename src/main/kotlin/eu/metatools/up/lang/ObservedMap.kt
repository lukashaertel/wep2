package eu.metatools.up.lang

import java.util.*

/**
 * Delegates to a [NavigableMap] [actual] to implement the behavior. All adds, changes and removes are tracked
 * to [notify].
 * @property actual The actual implementation.
 * @property notify The listener.
 */
class ObservedMap<K : Comparable<K>, V>(
    val actual: NavigableMap<K, V>,
    val notify: (SortedMap<K, V>, SortedMap<K, V>) -> Unit
) : NavigableMap<K, V> {
    init {
        // Notify initial value assignment.
        if (actual.isNotEmpty())
            notify(actual.toSortedMap(), sortedMapOf())
    }

    override fun containsValue(value: V) =
        // Check if actual contains value.
        actual.containsValue(value)

    override fun clear() {
        // Copy this map as the set of removed items, clear the actual map.
        val removed = actual.toSortedMap()
        actual.clear()

        // If actually removed, notify.
        if (removed.isNotEmpty())
            notify(sortedMapOf(), removed)
    }

    override fun floorKey(key: K): K? =
        // Get floor key in actual map.
        actual.floorKey(key)

    override fun floorEntry(key: K) =
        // Get floor entry in actual map, wrap entry if present.
        actual.floorEntry(key)?.let { wrapEntry(it) }

    override fun lastKey(): K =
        // Get last key in actual map.
        actual.lastKey()

    override fun containsKey(key: K): Boolean =
        // Check if actual contains key.
        actual.containsKey(key)

    override fun get(key: K): V? =
        // Return the actual value at the key.
        actual[key]

    override fun putAll(from: Map<out K, V>) {
        // Skip routine if not adding anything.
        if (from.isEmpty())
            return

        // Track adds and changes.
        val added = TreeMap<K, V>()
        val removed = TreeMap<K, V>()

        // Iterate all items to put.
        for ((key, value) in from) {
            // Assign new value.
            val before = actual.put(key, value)

            // No actual change, skip this entry.
            if (before == value)
                continue

            // See if value was added or changed.
            if (before != null)
                removed[key] = before

            added[key] = value
        }

        // If anything added or removed, notify.
        if (added.isNotEmpty() || removed.isNotEmpty())
            notify(added, removed)
    }

    override fun higherEntry(key: K) =
        // Get higher entry in actual map, wrap entry if present.
        actual.higherEntry(key)?.let { wrapEntry(it) }

    override fun descendingKeySet() =
        // Wrap the descending keys of the actual map.
        wrapKeySet(actual.descendingKeySet())

    override fun navigableKeySet() =
        // Wrap the key set of the actual map.
        wrapKeySet(actual.navigableKeySet())

    override fun subMap(fromKey: K, fromInclusive: Boolean, toKey: K, toInclusive: Boolean) =
        // Observe part of the map.
        ObservedMap(actual.subMap(fromKey, fromInclusive, toKey, toInclusive), notify)

    override fun subMap(fromKey: K, toKey: K) =
        // Observe part of the map.
        subMap(fromKey, true, toKey, false)

    override fun tailMap(fromKey: K, inclusive: Boolean) =
        // Observe part of the map.
        ObservedMap(actual.tailMap(fromKey, inclusive), notify)

    override fun tailMap(fromKey: K) =
        // Observe part of the map.
        tailMap(fromKey, true)

    override fun pollLastEntry(): MutableMap.MutableEntry<K, V>? {
        // Remove last entry, will be disconnected so no wrapping is needed.
        val result = actual.pollLastEntry()

        // If there was an entry, notify change.
        if (result != null)
            notify(sortedMapOf(), sortedMapOf(result.key to result.value))

        // Return polled entry.
        return result
    }

    override fun headMap(toKey: K, inclusive: Boolean) =
        // Observe part of the map.
        ObservedMap(actual.headMap(toKey, inclusive), notify)

    override fun headMap(toKey: K) =
        // Observe part of the map.
        headMap(toKey, false)

    override fun pollFirstEntry(): MutableMap.MutableEntry<K, V>? {
        // Remove first entry, will be disconnected so no wrapping is needed.
        val result = actual.pollFirstEntry()

        // If there was an entry, notify change.
        if (result != null)
            notify(sortedMapOf(), sortedMapOf(result.key to result.value))

        // Return polled entry.
        return result
    }

    override fun descendingMap() =
        // Observe the descending actual map.
        ObservedMap(actual.descendingMap(), notify)

    override fun lastEntry() =
        // Return the last entry of the actual map, wrap if present.
        actual.lastEntry()?.let { wrapEntry(it) }

    override fun isEmpty() =
        // Empty if actual map is entry.
        actual.isEmpty()

    override fun put(key: K, value: V): V? {
        // Put the value in the actual map, get previous association.
        val before = actual.put(key, value)

        // Check if processing as change or add.
        if (before == null)
            notify(sortedMapOf(key to value), sortedMapOf())
        else
            notify(sortedMapOf(key to value), sortedMapOf(key to before))

        // Return the original value.
        return before
    }

    override fun remove(key: K): V? {
        // Remove the actual value for it's association.
        val before = actual.remove(key)

        // If value was present, notify removal.
        if (before != null)
            notify(sortedMapOf(), sortedMapOf(key to before))

        // Return the previous association.
        return before
    }

    override fun comparator(): Comparator<in K> =
        // Return the actual map's comparator.
        actual.comparator()

    override fun lowerKey(key: K): K? =
        // Get lower key in actual map.
        actual.lowerKey(key)

    override fun ceilingEntry(key: K) =
        // Return the ceiling entry of the actual map, wrap if present.
        actual.ceilingEntry(key)?.let { wrapEntry(it) }

    override fun firstEntry() =
        // Return the first entry of the actual map, wrap if present.
        actual.firstEntry()?.let { wrapEntry(it) }

    override fun lowerEntry(key: K) =
        // Return the lower entry in the actual map, wrap if present.
        actual.lowerEntry(key)?.let { wrapEntry(it) }

    override fun ceilingKey(key: K): K? =
        // Return the ceiling key of the actual map.
        actual.ceilingKey(key)

    override fun firstKey(): K =
        // Return the first key of the actual map.
        actual.firstKey()

    override fun higherKey(key: K): K? =
        // Return the higher key of the actual map.
        actual.higherKey(key)

    override val entries = object : MutableSet<MutableMap.MutableEntry<K, V>> {
        override fun add(element: MutableMap.MutableEntry<K, V>): Boolean {
            // Put entry.
            val before = actual.put(element.key, element.value)

            // If value did not change, skip notification.
            if (before == element.value)
                return false

            // Notify according to value is new or is changed.
            if (before == null)
                notify(sortedMapOf(element.key to element.value), sortedMapOf())
            else
                notify(sortedMapOf(element.key to element.value), sortedMapOf(element.key to before))

            // Return true as value was changed..
            return true
        }

        override fun addAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean {
            // Elements empty, no change will happen.
            if (elements.isEmpty())
                return false

            // Track adds and changes.
            val added = TreeMap<K, V>()
            val removed = TreeMap<K, V>()

            // Set all elements.
            for ((key, value) in elements) {
                // Actually put value, if equal mapping, skip this entry.
                val before = actual.put(key, value)
                if (before == value)
                    continue

                // Assign to the proper notification basket.
                if (before != null)
                    removed[key] = before

                added[key] = value
            }

            // If no change, return false.
            if (added.isEmpty() && removed.isEmpty())
                return false

            // Notify change, return true.
            notify(added, removed)
            return true

        }

        override fun clear() {
            // Clear in observed map.
            this@ObservedMap.clear()
        }

        override fun iterator() = object : MutableIterator<MutableMap.MutableEntry<K, V>> {
            /**
             * Get base iterator.
             */
            val iterator = actual.entries.iterator()

            /**
             * The value that was currently iterated.
             */
            lateinit var current: MutableMap.MutableEntry<K, V>

            override fun hasNext() =
                // See if base iterator has next value.
                iterator.hasNext()

            override fun next() =
                // Wrap the result of getting the next element in the base iterator, also assign the current value.
                wrapEntry(iterator.next().also {
                    current = it
                })

            override fun remove() {
                // Remove in the base iterator.
                iterator.remove()

                // Notify the change with the remembered current value.
                notify(sortedMapOf(), sortedMapOf(current.key to current.value))
            }
        }

        override fun remove(element: MutableMap.MutableEntry<K, V>) =
            // Delegate directly to remove.
            this@ObservedMap.remove(element.key, element.value)

        override fun removeAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean {
            // No element removed, no change will happen.
            if (elements.isEmpty())
                return false

            // Track remove.
            val removed = TreeMap<K, V>()

            actual.entries.iterator().let {
                // Use mutable iteration.
                while (it.hasNext()) {
                    //Get current element.
                    val current = it.next()

                    // Check if the element is in the elements to remove.
                    if (current in elements) {
                        removed[current.key] = current.value
                        it.remove()
                    }
                }
            }

            // If nothing is removed, return false.
            if (removed.isEmpty())
                return false

            // Notify and return changed.
            notify(sortedMapOf(), removed)
            return true
        }

        override fun retainAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean {
            // Check if no elements are to be retained.
            if (elements.isEmpty()) {
                // If already empty, return unchanged.
                if (isEmpty())
                    return false

                // Clear entries, return changed.
                clear()
                return true
            }

            // Track removals.
            val removed = TreeMap<K, V>()

            actual.entries.iterator().let {
                // Use mutable iteration.
                while (it.hasNext()) {
                    //Get current element.
                    val current = it.next()

                    // Check if the element is not in the elements to retain.
                    if (current !in elements) {
                        removed[current.key] = current.value
                        it.remove()
                    }
                }
            }

            // If nothing was removed, return false.
            if (removed.isEmpty())
                return false

            // Notify removal, return true.
            notify(sortedMapOf(), removed)
            return true
        }

        override val size: Int
            get() =
                // Delegate size to actual entries.
                actual.entries.size

        override fun contains(element: MutableMap.MutableEntry<K, V>) =
            // Delegate contains to actual entries.
            actual.entries.contains(element)

        override fun containsAll(elements: Collection<MutableMap.MutableEntry<K, V>>) =
            // Delegate contains all to actual entries.
            actual.entries.containsAll(elements)

        override fun isEmpty() =
            // Delegate is empty to actual entries.
            actual.entries.isEmpty()

        override fun hashCode() =
            actual.entries.hashCode()

        override fun equals(other: Any?) =
            actual.entries == other

        override fun toString() =
            actual.entries.toString()
    }

    override val keys: MutableSet<K>
        get() =
            // Use navigable key set, not differences in implementation.
            navigableKeySet()

    override val values = object : MutableCollection<V> {
        override val size: Int
            get() =
                // Delegate size to actual values.
                actual.values.size

        override fun contains(element: V): Boolean =
            // Delegate contains to actual values.
            actual.values.contains(element)

        override fun containsAll(elements: Collection<V>) =
            // Delegate contains all to actual values.
            actual.values.containsAll(elements)

        override fun isEmpty() =
            // Delegate is empty to actual values.
            actual.values.isEmpty()

        override fun add(element: V) =
            // Operation not supported per specification.
            throw UnsupportedOperationException()

        override fun addAll(elements: Collection<V>) =
            // Operation not supported per specification.
            throw UnsupportedOperationException()

        override fun clear() {
            // Clear in observed map.
            this@ObservedMap.clear()
        }

        override fun iterator() = object : MutableIterator<V> {
            /**
             * Get base iterator.
             */
            val iterator = actual.entries.iterator()

            /**
             * The current key-value assignment.
             */
            lateinit var current: MutableMap.MutableEntry<K, V>

            override fun hasNext() =
                // Return base iterator element availability.
                iterator.hasNext()

            override fun next() =
                // Iterate next element, remember entry.
                iterator.next().also {
                    current = it
                }.value

            override fun remove() {
                // Remove on base iterator.
                iterator.remove()

                // Notify the change with the remembered current value.
                notify(sortedMapOf(), sortedMapOf(current.key to current.value))
            }
        }

        override fun remove(element: V): Boolean {
            // Track removed entries.
            val removed = TreeMap<K, V>()

            actual.entries.iterator().let {
                // Use mutable iteration.
                while (it.hasNext()) {
                    val current = it.next()

                    // Remove if value the element to remove.
                    if (current.value == element) {
                        removed[current.key] = current.value
                        it.remove()
                    }
                }
            }

            // Nothing removed, return false.
            if (removed.isEmpty())
                return false

            // Removal occurred, notify and return true.
            notify(sortedMapOf(), removed)
            return true
        }

        override fun removeAll(elements: Collection<V>): Boolean {
            // Check if no elements are to be removed.
            if (elements.isEmpty())
                return false

            // Track removed entries and if actual has changed.
            val removed = TreeMap<K, V>()

            actual.entries.iterator().let {
                // Use mutable iteration.
                while (it.hasNext()) {
                    val current = it.next()

                    // Remove if value in elements to remove.
                    if (current.value in elements) {
                        removed[current.key] = current.value
                        it.remove()
                    }
                }
            }

            // Nothing removed, return false.
            if (removed.isEmpty())
                return false

            // Removal occurred, notify and return true.
            notify(sortedMapOf(), removed)
            return true
        }

        override fun retainAll(elements: Collection<V>): Boolean {
            // Check if no elements are to be retained.
            if (elements.isEmpty()) {
                // If already empty, return unchanged.
                if (isEmpty())
                    return false

                // Clear values, return changed.
                clear()
                return true
            }

            // Track removed entries and if actual has changed.
            val removed = TreeMap<K, V>()

            actual.entries.iterator().let {
                // Use mutable iteration.
                while (it.hasNext()) {
                    val current = it.next()

                    // Remove if value not in elements to retain.
                    if (current.value !in elements) {
                        removed[current.key] = current.value
                        it.remove()
                    }
                }
            }

            // Nothing removed, return false.
            if (removed.isEmpty())
                return false

            // Removal occurred, notify and return true.
            notify(sortedMapOf(), removed)
            return true
        }

        override fun hashCode() =
            actual.values.hashCode()

        override fun equals(other: Any?) =
            actual.values == other

        override fun toString() =
            actual.values.toString()
    }

    override val size: Int
        get() =
            // Delegate size to actual map.
            actual.size

    /**
     * Wraps an entry to generate notifications on proper changes.
     */
    private fun wrapEntry(entry: MutableMap.MutableEntry<K, V>) = object : MutableMap.MutableEntry<K, V> {
        override val key: K
            get() = entry.key

        override var value: V = entry.value

        override fun setValue(newValue: V): V {
            // No change, skip routine.
            if (newValue == value)
                return value

            // Write backing.
            actual[key] = newValue

            // Memorize and change value.
            val previous = value
            value = newValue

            // Notify change.
            notify(sortedMapOf(key to newValue), sortedMapOf(key to previous))
            return previous
        }
    }

    /**
     * Wraps a key set to notifications on proper changes.
     */
    private fun wrapKeySet(keySet: NavigableSet<K>) = ObservedSet(keySet) { add, remove ->
        // Non-legal modification.
        if (add.isNotEmpty()) throw UnsupportedOperationException()

        // Collect all actually removed.
        val removed = TreeMap<K, V>()
        for (key in remove) {
            val before = actual.remove(key)
            if (before != null)
                removed[key] = before
        }

        // Notify.
        if (removed.isNotEmpty())
            notify(sortedMapOf(), removed)
    }

    override fun hashCode() =
        actual.hashCode()

    override fun equals(other: Any?) =
        actual == other

    override fun toString() =
        actual.toString()
}