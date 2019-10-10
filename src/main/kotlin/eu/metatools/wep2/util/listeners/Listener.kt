package eu.metatools.wep2.util.listeners

/**
 * Listens to single value changes.
 */
interface Listener<in R, in V> {
    companion object {
        /**
         * A listener that does nothing.
         */
        val EMPTY = object : Listener<Any?, Any?> {
            override fun initialized(receiver: Any?, value: Any?) = Unit

            override fun changed(receiver: Any?, oldValue: Any?, newValue: Any?) = Unit
        }

        /**
         * A listener that prints to console.
         */
        val CONSOLE = console("subject")

        /**
         * A listener that listens to changes and reports them under [name].
         */
        fun console(name: String) = object : Listener<Any?, Any?> {
            override fun initialized(receiver: Any?, value: Any?) {
                println("initialized $name=$value")
            }

            override fun changed(receiver: Any?, oldValue: Any?, newValue: Any?) {
                println("changed $name=$newValue, was $oldValue")
            }
        }
    }

    /**
     * Called when a value is first set.
     */
    fun initialized(receiver: R, value: V)

    /**
     * Called when a value is changed.
     */
    fun changed(receiver: R, oldValue: V, newValue: V)
}

/**
 * Creates a listener sequence of the receiver and [other].
 */
operator fun <R, V> Listener<R, V>.plus(other: Listener<R, V>) =
    object : Listener<R, V> {
        override fun initialized(receiver: R, value: V) {
            this@plus.initialized(receiver, value)
            other.initialized(receiver, value)
        }

        override fun changed(receiver: R, oldValue: V, newValue: V) {
            this@plus.changed(receiver, oldValue, newValue)
            other.changed(receiver, oldValue, newValue)
        }
    }

/**
 * Creates a new listener with the given implementations.
 */
inline fun <R, V> listener(
    crossinline initialized: R.(V) -> Unit = {},
    crossinline changed: R.(V, V) -> Unit = { _, _ -> }
) = object : Listener<R, V> {
    override fun initialized(receiver: R, value: V) =
        initialized(receiver, value)

    override fun changed(receiver: R, oldValue: V, newValue: V) =
        changed(receiver, oldValue, newValue)
}