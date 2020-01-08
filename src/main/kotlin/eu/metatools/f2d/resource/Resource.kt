package eu.metatools.f2d.resource

/**
 * A resource that can be instantiated with an argument.
 */
interface Resource<in A, out I> {
    /**
     * Creates an instance referring to the definition of this resource.
     */
    operator fun get(argsResource: A): I
}

/**
 * For a resource with a [Unit] argument, refers with argument [Unit] passed.
 */
fun <I> Resource<Unit, I>.get() =
    get(Unit)

/**
 * For a resource with a nullable argument, refers with argument `null` passed.
 */
@JvmName("referNullArg")
fun <T, I> Resource<T?, I>.get() =
    get(null)