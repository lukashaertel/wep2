package eu.metatools.fio2

import java.util.*

/**
 * Allows for referring and releasing consecutive addresses.
 */
class AddressGenerator {
    /**
     * Set of open location used for reuse.
     */
    private val open = TreeSet<Int>()

    /**
     * The location after the last element. Essentially count of fully used locations.
     */
    var end = 0
        private set

    /**
     * Returns a new or existing open location.
     */
    fun refer(): Int {
        // Poll an existing location or return new end.
        return open.pollLast() ?: end++
    }

    /**
     * Releases a location to be reused.
     */
    fun release(reference: Int) {
        // Add open location.
        open.add(reference)

        // Collapse end.
        val iterator = open.descendingIterator()
        while (iterator.hasNext()) {
            // If open stack does not coincide with end, cannot consolidate.
            val at = iterator.next()
            if (at != end - 1)
                break

            // Coinciding, remove stack end and decrement end.
            iterator.remove()
            end--
        }
    }

    fun references() = 0.until(end)
            .filter { it !in open }
}