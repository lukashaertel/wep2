package eu.metatools.up

import eu.metatools.up.dt.Instruction
import eu.metatools.up.dt.Lx
import eu.metatools.up.notify.Callback

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

/**
 * Directs a wrapper on the [Engine]'s [Engine.onAdd] handler, invoked when the [Driver.ent] is the subject.
 */
val Driver.onAttached
    get() = object : Callback {
        override fun register(handler: () -> Unit) =
            ent.shell.engine.onAdd.register { _, subject -> if (ent === subject) handler() }
    }

/**
 * Directs a wrapper on the [Engine]'s [Engine.onRemove] handler, invoked when the [Driver.ent] is the subject.
 */
val Driver.onDetached
    get() = object : Callback {
        override fun register(handler: () -> Unit) =
            ent.shell.engine.onRemove.register { _, subject -> if (ent === subject) handler() }
    }
