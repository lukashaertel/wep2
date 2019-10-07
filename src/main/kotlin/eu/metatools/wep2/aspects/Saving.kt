package eu.metatools.wep2.aspects

import eu.metatools.wep2.storage.Store

/**
 * Marks an object that supports amending the save routine.
 */
interface Saving {
    /**
     * Adds this routine to the way the receiver is saved.
     */
    fun saveWith(block: (Store) -> Unit)
}