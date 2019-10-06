package eu.metatools.wep2.storage

import eu.metatools.wep2.entity.RestoringEntity

/**
 * Restores values, used in combination with [RestoringEntity].
 */
interface Restore {
    /**
     * Loads a value for the key.
     */
    fun <T> load(key: String): T

    /**
     * Registers a post-restore callback.
     */
    fun registerPost(block: () -> Unit)
}

/**
 * Creates a nested restore object, redirecting loads to a subset of keys.
 */
fun Restore.path(prefix: String) = object : Restore {
    override fun <T> load(key: String) =
        this@path.load<T>("$prefix/$key")

    override fun registerPost(block: () -> Unit) =
        this@path.registerPost(block)
}


/**
 * Creates a basic restore interface that [load]s the given keys and collects post-operations. Runs the [block] with it
 * and performs all collected post-operations.
 */
inline fun <R> restoreBy(crossinline load: (String) -> Any?, block: (Restore) -> R): R {
    // Collect all post-operations.
    val post = mutableListOf<() -> Unit>()

    // Create restore object, collecting to post and delegating to load.
    val restore = object : Restore {
        @Suppress("unchecked_cast")
        override fun <T> load(key: String) = load(key) as T

        override fun registerPost(block: () -> Unit) {
            post.add(block)
        }
    }

    // Invoke the block.
    val result = block(restore)

    // execute post options.
    post.forEach {
        it()
    }

    // Return result of the block.
    return result
}

