package eu.metatools.up.aspects

import eu.metatools.up.dt.Lx

/**
 * Tracks resetting of identified components.
 */
interface Track : Aspect {
    /**
     * Appends a change undo method to the identified component.
     */
    fun resetWith(id: Lx, undo: () -> Unit)
}