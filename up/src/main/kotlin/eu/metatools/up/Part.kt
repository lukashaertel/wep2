package eu.metatools.up

import eu.metatools.up.dt.Change

/**
 * A part of an [Ent]s [Driver].
 */
interface Part {
    /**
     * True if connected.
     */
    val isConnected: Boolean

    /**
     * ID of the part.
     */
    val name: String // TODO: Investigate, replace with compact structure.

    /**
     * Handle to notify changes to.
     */
    var notifyHandle: (name: String, change: Change<*>) -> Unit

    /**
     * Invoked when parent [Ent] is connected or when inserting into a connected [Ent].
     */
    fun connect(partIn: PartIn?)

    /**
     * Saves the part to the output.
     */
    fun persist(partOut: PartOut)

    /**
     * Invoked when parent [Ent] is disconnected or when removing from a connected [Ent].
     */
    fun disconnect()

    /**
     * Called when the entire system is connected.
     */
    fun ready()
}