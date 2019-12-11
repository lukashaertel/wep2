package eu.metatools.f2d.context

/**
 * Interface of exposed lifecycle handlers.
 */
interface Lifecycle {
    /**
     * Loads the underlying data.
     */
    fun initialize()

    /**
     * Disposes of the resource.
     */
    fun dispose()
}

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
 * A resource with lifecycle.
 */
interface LifecycleResource<in A, out I> : Lifecycle, Resource<A, I>

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

/**
 * A drawable with lifecycle.
 */
interface LifecycleDrawable<in T> : Lifecycle, Drawable<T>

/**
 * A playable with lifecycle.
 */
interface LifecyclePlayable<in T> : Lifecycle, Playable<T>

/**
 * For a resource with a [Unit] argument, refers with argument [Unit] passed.
 */
fun <I> Resource<Unit, I>.refer() =
    get(Unit)

/**
 * For a resource with a nullable argument, refers with argument `null` passed.
 */
@JvmName("referNullArg")
fun <T, I> Resource<T?, I>.refer() =
    get(null)