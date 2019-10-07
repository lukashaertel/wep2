package eu.metatools.wep2.aspects

import eu.metatools.wep2.storage.Store
import eu.metatools.wep2.storage.storeBy

/**
 * Marks an object that supports amending the save routine.
 */
interface Saving {
    /**
     * Adds this routine to the way the receiver is saved.
     */
    fun saveWith(block: (Store) -> Unit)

    /**
     * Performs the actual saving to the store.
     */
    fun save(store: Store)
}

/**
 * Saves the receiver to a map.
 */
fun Saving.saveToMap() = mutableMapOf<String, Any?>().also {
    storeBy(it::set, this::save)
}
