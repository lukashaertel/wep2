package eu.metatools.up.notify

/**
 * A general callback.
 */
interface Callback : () -> Unit {
    /**
     * Registers a callback, returns an auto closable removing it.
     */
    fun register(handler: () -> Unit): AutoCloseable
}

/**
 * A general event implemented by a handler-list.
 */
class CallbackList : Callback {
    /**
     * Private list of handlers.
     */
    private val handlers = mutableListOf<() -> Unit>()

    /**
     * Registers a callback, returns an auto closable removing it.
     */
    override fun register(handler: () -> Unit): AutoCloseable {
        // Add handler.
        handlers.add(handler)

        // Return removing the handler.
        return AutoCloseable {
            handlers.remove(handler)
        }
    }

    /**
     * Runs all callbacks in the sequence they were added.
     */
    override fun invoke() {
        handlers.forEach { it() }
    }
}