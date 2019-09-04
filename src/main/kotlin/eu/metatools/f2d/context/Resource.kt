package eu.metatools.f2d.context

interface Resource<I> {
    /**
     * Loads the underlying data.
     */
    fun initialize()

    /**
     * Disposes of the resource.
     */
    fun dispose()

    /**
     * Creates an instance referring to the definition of this resource.
     */
    fun refer(): I
}