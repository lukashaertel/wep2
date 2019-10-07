package eu.metatools.wep2.aspects

import eu.metatools.wep2.storage.Restore

/**
 * Marks an object that supports restoring.
 */
interface Restoring {
    /**
     * The restore source or `null` if not restoring.
     */
    val restore: Restore?
}

/**
 * If true, the receiver was restored instead of created.
 */
val Restoring.wasRestored get() = restore != null

