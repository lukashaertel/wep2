package eu.metatools.wep2.nes.notify

/**
 * A general event.
 */
class Handler<T> : (T) -> Unit {
    /**
     * Private list of handlers.
     */
    private val handlers = mutableListOf<(T) -> Unit>()

    /**
     * Registers a handler, returns an auto closable removing it.
     */
    fun register(handler: (T) -> Unit): AutoCloseable {
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