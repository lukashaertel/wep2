package eu.metatools.wep2.nes.aspects

import eu.metatools.wep2.nes.dt.Lx

interface Track : Aspect {
    fun resetWith(id: Lx, undo: () -> Unit)
}