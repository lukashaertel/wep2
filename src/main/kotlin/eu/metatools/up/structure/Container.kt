package eu.metatools.wep2.nes.structure

import eu.metatools.wep2.nes.aspects.Aspect
import eu.metatools.wep2.nes.dt.Lx

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

interface Container : Aspect, Id {
    /**
     * Resolves an absolute [id] to a [Part].
     */
    fun resolve(id: Lx): Part?

    /**
     * Includes a [part] with the given absolute [id].
     */
    fun include(id: Lx, part: Part)
}
