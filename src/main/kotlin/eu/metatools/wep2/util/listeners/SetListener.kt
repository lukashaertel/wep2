package eu.metatools.wep2.util.listeners

/**
 * Listens to collection changes.
 */
interface SetListener<in R, in E> {
    companion object {
        /**
         * A listener that does nothing.
         */
        val EMPTY = object : SetListener<Any?, Any?> {
            override fun added(receiver: Any?, element: Any?) = Unit

            override fun removed(receiver: Any?, element: Any?) = Unit
        }
    }

    /**
     * Called when an element is actually added.
     */
    fun added(receiver: R, element: E)

    /**
     * Called when an element is actually removed.
     */
    fun removed(receiver: R, element: E)
}

/**
 * Creates a listener sequence of the receiver and [other].
 */
operator fun <R, E> SetListener<R, E>.plus(other: SetListener<R, E>) =
    object : SetListener<R, E> {
        override fun added(receiver: R, element: E) {
            this@plus.added(receiver, element)
            other.added(receiver, element)
        }

        override fun removed(receiver: R, element: E) {
            this@plus.removed(receiver, element)
            other.removed(receiver, element)
        }
    }

/**
 * Creates a new listener with the given implementations.
 */
inline fun <R, E> setListener(
    crossinline added: R.(E) -> Unit = {},
    crossinline removed: R.(E) -> Unit = {}
) = object : SetListener<R, E> {
    override fun added(receiver: R, element: E) =
        added(receiver, element)

    override fun removed(receiver: R, element: E) =
        removed(receiver, element)
}