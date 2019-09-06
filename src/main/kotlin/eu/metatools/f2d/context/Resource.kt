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
    fun refer(argsResource: A): I
}

/**
 * A resource with lifecycle.
 */
interface LifecycleResource<in A, out I> : Lifecycle, Resource<A, I>

/**
 * A resource that handles lifecycle methods and passes them to the created instances.
 */
abstract class NotifyingResource<in A, out I : Lifecycle> : LifecycleResource<A, I> {
    /**
     * Handles initialization of the local resource.
     */
    protected abstract fun initializeSelf()

    /**
     * Handles disposition of the local resource.
     */
    protected abstract fun disposeSelf()

    /**
     * Refers a new instance.
     */
    protected abstract fun referNew(argsResource: A): I

    /**
     * True if resource is initialized.
     */
    var isInitialized = false
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
        isInitialized = false
        created.forEach(Lifecycle::dispose)
        disposeSelf()
    }

    override fun refer(argsResource: A) =
        // Create a new element, also add it to the set.
        referNew(argsResource).also {
            created.add(it)
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
    refer(Unit)

/**
 * For a resource with a nullable argument, refers with argument `null` passed.
 */
@JvmName("referNullArg")
fun <T, I> Resource<T?, I>.refer() =
    refer(null)