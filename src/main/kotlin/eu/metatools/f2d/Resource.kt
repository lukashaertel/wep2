package eu.metatools.f2d

interface Resource {
    /**
     * Loads the underlying data.
     */
    fun initialize()

    /**
     * Disposes of the resource.
     */
    fun dispose()
}

interface Instantiable<A, I> {
    /**
     * Creates an instance of this resource.
     */
    fun instantiate(arguments: A): I
}

interface Lifetime {
    /**
     * True if this object has started.
     */
    fun hasStarted(time: Double): Boolean

    /**
     * True if this object has ended for the time.
     */
    fun hasEnded(time: Double): Boolean
}