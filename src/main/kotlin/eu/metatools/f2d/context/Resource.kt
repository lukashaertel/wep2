package eu.metatools.f2d.context

interface Resource<in A, out I> {
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
    fun refer(arguments: A): I
}

/**
 * For a resource with a [Unit] argument, refers with argument [Unit] passed.
 */
fun <I> Resource<Unit, I>.refer() =
    refer(Unit)

/**
 * For a resource with a nullable argument, refers with argument `null` passed.
 */
@JvmName("referNullArg")
fun <T, I> Resource<T?, I>.refer() =
    refer(null)