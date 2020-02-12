package eu.metatools.fio.resource

/**
 * A standard resource that remembers the results per resource argument.
 */
abstract class MemorizingResource<in A, out I> : LifecycleResource<A, I> {
    /**
     * The elements created.
     */
    private val existing = hashMapOf<A, I>()

    /**
     * Refers a new instance.
     */
    protected abstract fun referNew(argsResource: A): I

    /**
     * Gets or creates the subject for the argument.
     */
    override fun get(argsResource: A) =
        existing.getOrPut(argsResource) { referNew(argsResource) }
}