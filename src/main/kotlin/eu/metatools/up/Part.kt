package eu.metatools.up

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
     * Invoked when parent [Ent] is connected or when inserting into a connected [Ent].
     */
    fun connect(partIn: PartIn?)

    fun persist(partOut: PartOut)

    /**
     * Invoked when parent [Ent] is disconnected or when removing from a connected [Ent].
     */
    fun disconnect()
}