package eu.metatools.up

/**
 * A part of an [Ent]s [Driver].
 */
interface Part {
    /**
     * Invoked when parent [Ent] is connected or when inserting into a connected [Ent].
     */
    fun connect()

    /**
     * Invoked when parent [Ent] is disconnected or when removing from a connected [Ent].
     */
    fun disconnect()
}