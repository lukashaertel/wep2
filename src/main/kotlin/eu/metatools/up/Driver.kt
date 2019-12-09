package eu.metatools.up

import eu.metatools.up.dt.Instruction
import eu.metatools.up.dt.Lx
import eu.metatools.up.lang.validate

/**
 * Driver implementing actual entity administrative options.
 */
interface Driver {
    /**
     * The actual entity that is driven.
     */
    val ent: Ent

    /**
     * True if connected.
     */
    val isConnected: Boolean

    /**
     * Adds a part to the entity. This maintains connection status and allows resolution. Automatically performed by
     * the property creators and DSL methods.
     */
    fun configure(part: Part)

    /**
     * Connects this entity. Automatically called on [Ent.constructed] and [Ent.delete].
     */
    fun connect(entIn: EntIn?)

    fun persist(entOut: EntOut)

    /**
     * Disconnects this entity. Automatically called on [Ent.constructed] and [Ent.delete].
     */
    fun disconnect()

    /**
     * Instruction-in node. Called by registered handlers.
     */
    fun perform(instruction: Instruction)
}

fun Driver.requireUnconnected() =
    validate(!isConnected) { "Driver may not be connected." }

fun Driver.requireConnected(): Nothing? =
    validate(isConnected) { "Driver must be connected." }