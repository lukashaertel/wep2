package eu.metatools.f2d.resource

/**
 * A resource that handles lifecycle methods and passes them to the created instances.
 */
abstract class NotifyingResource<in A, out I : Lifecycle> : MemorizingResource<A, I>() {
    /**
     * Handles initialization of the local resource. Does nothing on default if no resource level disposable is used.
     */
    protected open fun initializeSelf() {}

    /**
     * Handles disposition of the local resource. Does nothing on default if no resource level disposable is used.
     */
    protected open fun disposeSelf() {}

    /**
     * True if resource is initialized.
     */
    var isInitialized = false
        private set

    /**
     * True if resource is disposed.
     */
    var isDisposed = false
        private set

    /**
     * Set of created objects.
     */
    private val created = mutableSetOf<Lifecycle>()

    override fun initialize() {
        initializeSelf()
        created.forEach(Lifecycle::initialize)
        isInitialized = true
    }

    override fun dispose() {
        created.forEach(Lifecycle::dispose)
        disposeSelf()
        isDisposed = true
    }

    override fun get(argsResource: A) =
        // Let super get or create new subject.
        super.get(argsResource).also {
            // Add it to the set of created instances to dispose later.
            created.add(it)

            // If already initialized but not yet disposed, initialize the instance
            // on the spot.
            if (isInitialized && !isDisposed)
                it.initialize()
        }
}