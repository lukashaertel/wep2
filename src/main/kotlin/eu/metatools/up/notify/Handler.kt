package eu.metatools.up.notify

/**
 * A general event.
 */
interface Handler<T> {
    /**
     * Registers a handler, returns an auto closable removing it.
     */
    fun register(handler: (T) -> Unit): AutoCloseable
}

/**
 * Registers a self-removing handler.
 */
fun <T> Handler<T>.registerOnce(handler: (T) -> Unit) {
    lateinit var closable: AutoCloseable
    closable = register { t ->
        closable.close()
        handler(t)
    }
}

/**
 * A general event implemented by a handler-list.
 */
class HandlerList<T> : Handler<T>, (T) -> Unit {
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