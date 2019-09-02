package eu.metatools.wep2.entity.bind

import eu.metatools.wep2.entity.RestoringEntity

/**
 * Stores values, used in combination with [RestoringEntity].
 */
interface Store {
    /**
     * Saves a value for a key.
     */
    fun <T> save(key: String, value: T)
}

/**
 * Creates a nested store object, redirecting loads to a subset of keys.
 */
fun Store.path(prefix: String) = object : Store {
    override fun <T> save(key: String, value: T) {
        this@path.save("$prefix/$key", value)
    }
}

/**
 * Creates a basic store interface that [save]s the given items. Runs the [block] with it.
 */
inline fun storeBy(crossinline save: (String, Any?) -> Unit, block: (Store) -> Unit) {
    // Create store object, delegating to save.
    val store = object : Store {
        override fun <T> save(key: String, value: T) {
            save(key, value)
        }
    }

    // Invoke the block.
    block(store)
}
