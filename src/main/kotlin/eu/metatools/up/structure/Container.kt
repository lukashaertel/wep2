package eu.metatools.up.structure

import eu.metatools.up.aspects.Aspect
import eu.metatools.up.dt.Lx

interface Id : Aspect {
    /**
     * Absolute ID of the element.
     */
    val id: Lx
}

interface Part : Id {
    fun connect()

    fun disconnect()
}

/**
 * Runs the block with the part, calling [Part.connect] before invocation and [Part.disconnect] after invocation.
 */
inline fun <R> Part.connectIn(block: () -> R) =
    try {
        connect()
        block()
    } finally {
        disconnect()
    }


interface Container : Aspect, Id {
    /**
     * Resolves an absolute [id] to a [Part].
     */
    fun resolve(id: Lx): Part?

    /**
     * Includes a [part] with the given absolute [id].
     */
    fun include(id: Lx, part: Part)

    /**
     * Removes a [Part].
     */
    fun exclude(id: Lx)
}
