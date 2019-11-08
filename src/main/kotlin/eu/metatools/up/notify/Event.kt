package eu.metatools.wep2.nes.notify

/**
 * A keyed event.
 */
class Event<K, T> : (K, T) -> Unit {
    /**
     * Private map of keys to handlers.
     */
    private val handlers = mutableMapOf<K, MutableList<(T) -> Unit>>()

    /**
     * Registers a handler for the key, returns an auto closable removing it.
     */
    fun register(key: K, handler: (T) -> Unit): AutoCloseable {
        // Get the target list to put the handler in.
        val target = handlers.getOrPut(key, ::ArrayList)

        // Add handler.
        target.add(handler)

        // Return removing the handler and cleaning the key if necessary.
        return AutoCloseable {
            if (target.remove(handler) && target.isEmpty())
                handlers.remove(key)
        }
    }

    /**
     * Runs all handlers for the [key] with the [arg] in the sequence they were added.
     */
    override fun invoke(key: K, arg: T) {
        handlers[key]?.forEach { it(arg) }
    }
}