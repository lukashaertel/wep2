package eu.metatools.sx.index

/**
 * Provides the base implementation for registration and publication mechanisms, used
 * in root nodes.
 */
abstract class BaseIndex<K : Comparable<K>, V> : Index<K, V>() {
    /**
     * The registration of listeners.
     */
    private val registration = hashMapOf<Query<K>, MutableList<(K, Delta<V>) -> Unit>>()

    override fun register(query: Query<K>, block: (K, Delta<V>) -> Unit): AutoCloseable {
        // Get registration line for the query.
        val target = registration.getOrPut(query, ::ArrayList)

        // Add the receiver.
        target.add(block)

        // Return de-registration.
        return AutoCloseable {
            // Remove receiver, if empty remove registration line.
            if (target.remove(block) && target.isEmpty())
                registration.remove(query)
        }
    }

    /**
     * Sends the delta to the matching registrations.
     */
    protected fun publish(key: K, delta: Delta<V>) {
        // For all matching registrations publish delta.
        for ((q, ls) in registration)
            if (q(key))
                ls.forEach { it(key, delta) }
    }
}