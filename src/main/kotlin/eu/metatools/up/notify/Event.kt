package eu.metatools.up.notify

/**
 * A keyed event.
 */
interface Event<K, T> : (K, T) -> Unit {
    /**
     * Registers a handler for all keys, returns an auto closable removing it.
     */
    fun register(handler: (K, T) -> Unit): AutoCloseable

    /**
     * Registers a handler for the key, returns an auto closable removing it.
     */
    fun register(key: K, handler: (T) -> Unit): AutoCloseable
}

/**
 * A keyed event implemented by a handler-list.
 */
class EventList<K, T> : Event<K, T> {
    private val globalHandlers = mutableListOf<(K, T) -> Unit>()

    /**
     * Private map of keys to handlers.
     */
    private val localHandlers = mutableMapOf<K, MutableList<(T) -> Unit>>()

    /**
     * Registers a handler for all keys, returns an auto closable removing it.
     */
    override fun register(handler: (K, T) -> Unit): AutoCloseable {
        // Add handler.
        globalHandlers.add(handler)

        // Return removing the handler.
        return AutoCloseable {
            globalHandlers.remove(handler)
        }
    }

    /**
     * Registers a handler for the key, returns an auto closable removing it.
     */
    override fun register(key: K, handler: (T) -> Unit): AutoCloseable {
        // Get the target list to put the handler in.
        val target = localHandlers.getOrPut(key, ::ArrayList)

        // Add handler.
        target.add(handler)

        // Return removing the handler and cleaning the key if necessary.
        return AutoCloseable {
            if (target.remove(handler) && target.isEmpty())
                localHandlers.remove(key)
        }
    }

    /**
     * Runs all handlers for the [key] with the [arg] in the sequence they were added.
     */
    override fun invoke(key: K, arg: T) {
        // Invoke all global handlers.
        globalHandlers.forEach { it(key, arg) }

        // Invoke all local handlers.
        localHandlers[key]?.forEach { it(arg) }
    }
}