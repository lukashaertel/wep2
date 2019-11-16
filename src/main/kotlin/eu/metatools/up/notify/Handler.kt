package eu.metatools.up.notify

interface Handler<T> : (T) -> Unit {
    /**
     * Registers a handler, returns an auto closable removing it.
     */
    fun register(handler: (T) -> Unit): AutoCloseable
}

/**
 * A general event.
 */
class HandlerList<T> : Handler<T> {
    /**
     * Private list of handlers.
     */
    private val handlers = mutableListOf<(T) -> Unit>()

    /**
     * Registers a handler, returns an auto closable removing it.
     */
    override fun register(handler: (T) -> Unit): AutoCloseable {
        // Add handler.
        handlers.add(handler)

        // Return removing the handler.
        return AutoCloseable {
            handlers.remove(handler)
        }
    }

    /**
     * Runs all handlers with the [arg] in the sequence they were added.
     */
    override fun invoke(arg: T) {
        handlers.forEach { it(arg) }
    }
}