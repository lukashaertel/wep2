package eu.metatools.wep2.util.listeners

/**
 * Listens to key/value changes.
 */
interface MapListener<in R, in K, in V> {
    companion object {
        /**
         * A listener that does nothing.
         */
        val EMPTY = object : MapListener<Any?, Any?, Any?> {
            override fun added(receiver: Any?, key: Any?, value: Any?) = Unit

            override fun changed(receiver: Any?, key: Any?, oldValue: Any?, newValue: Any?) = Unit

            override fun removed(receiver: Any?, key: Any?, value: Any?) = Unit
        }

        /**
         * A listener that prints to console.
         */
        val CONSOLE = Listener.console("subject")

        /**
         * A listener that listens to changes and reports them under [name].
         */
        fun console(name: String) = object : MapListener<Any?, Any?, Any?> {
            override fun added(receiver: Any?, key: Any?, value: Any?) {
                if (receiver == Unit)
                    println("added $name[$key]=$value")
                else
                    println("added $receiver: $name[$key]=$value")
            }

            override fun changed(receiver: Any?, key: Any?, oldValue: Any?, newValue: Any?) {
                if (receiver == Unit)
                    println("changed $name[$key]=$newValue, was $oldValue")
                else
                    println("changed $receiver: $name[$key]=$newValue, was $oldValue")
            }

            override fun removed(receiver: Any?, key: Any?, value: Any?) {
                if (receiver == Unit)
                    println("removed $name[$key], was $value")
                else
                    println("removed $receiver: $name[$key], was $value")
            }
        }
    }

    /**
     * Called when an entry is actually added.
     */
    fun added(receiver: R, key: K, value: V)

    /**
     * Called when an entry is actually changed.
     */
    fun changed(receiver: R, key: K, oldValue: V, newValue: V)

    /**
     * Called when an entry is actually removed.
     */
    fun removed(receiver: R, key: K, value: V)
}

/**
 * Creates a listener sequence of the receiver and [other].
 */
operator fun <R, K, V> MapListener<R, K, V>.plus(other: MapListener<R, K, V>) =
    object : MapListener<R, K, V> {
        override fun added(receiver: R, key: K, value: V) {
            this@plus.added(receiver, key, value)
            other.added(receiver, key, value)
        }

        override fun changed(receiver: R, key: K, oldValue: V, newValue: V) {
            this@plus.changed(receiver, key, oldValue, newValue)
            other.changed(receiver, key, oldValue, newValue)
        }

        override fun removed(receiver: R, key: K, value: V) {
            this@plus.removed(receiver, key, value)
            other.removed(receiver, key, value)
        }
    }

/**
 * Creates a new listener with the given implementations.
 */
inline fun <R, K, V> mapListener(
    crossinline added: R.(K, V) -> Unit = { _, _ -> },
    crossinline changed: R.(K, V, V) -> Unit = { _, _, _ -> },
    crossinline removed: R.(K, V) -> Unit = { _, _ -> }
) = object : MapListener<R, K, V> {
    override fun added(receiver: R, key: K, value: V) =
        added(receiver, key, value)

    override fun changed(receiver: R, key: K, oldValue: V, newValue: V) =
        changed(receiver, key, oldValue, newValue)

    override fun removed(receiver: R, key: K, value: V) =
        removed(receiver, key, value)
}