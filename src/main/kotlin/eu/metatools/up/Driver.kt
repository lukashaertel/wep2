package eu.metatools.up

import eu.metatools.up.dt.Instruction
import eu.metatools.up.dt.Lx
import java.io.PrintStream
import java.io.PrintWriter
import java.io.Writer
import java.lang.Appendable

/**
 * Driver implementing actual entity adminstrative options.
 */
interface Driver {
    /**
     * Adds a part to the entity. This maintains connection status and allows resolution. Automatically performed by
     * the property creators and DSL methods.
     */
    fun include(id: Lx, part: Part)

    /**
     * Connects this entity. Automatically called on [Ent.constructed] and [Ent.delete].
     */
    fun connect()

    /**
     * Disconnects this entity. Automatically called on [Ent.constructed] and [Ent.delete].
     */
    fun disconnect()

    /**
     * Instruction-in node. Called by registered handlers.
     */
    fun perform(instruction: Instruction)
}