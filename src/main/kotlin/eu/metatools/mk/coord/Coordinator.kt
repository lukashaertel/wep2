package eu.metatools.mk.coord

abstract class Coordinator<N, T : Comparable<T>> {
    /**
     * Registers a listener for generated name/time/argument triples.
     * @return Returns the closable that will unregister the block.
     */
    abstract fun register(block: (N, T, Any?) -> Unit): AutoCloseable

    /**
     * Receives a name/time/argument triples to be processed.
     */
    abstract fun receive(name: N, time: T, args: Any?)

    /**
     * Dispatches and feeds back a name/time/argument triples to registrations and evaluations.
     */
    abstract fun publish(name: N, time: T, args: Any?)

    /**
     * Collects a local invocation to be distributed.
     * @param name The name of what to invoke.
     * @param time The time at which to evaluate.
     * @param args All arguments.
     */
    fun signal(name: N, time: T, args: Any?) {
        // Evaluate locally as if it was received from outside.
        receive(name, time, args)

        // Distribute to registrations.
        publish(name, time, args)
    }


    /**
     * Receives a list of name/time/argument triples. Default delegates to [receive], special
     * implementations may override for more efficient processing.
     */
    open fun receiveAll(namesTimesAndArgs: Sequence<Triple<N, T, Any?>>) {
        namesTimesAndArgs
            .sortedBy { it.second }
            .forEach { (name, time, args) ->
                receive(name, time, args)
            }
    }
}