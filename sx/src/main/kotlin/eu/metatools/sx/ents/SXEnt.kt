package eu.metatools.sx.ents

import eu.metatools.sx.SX
import eu.metatools.up.Ent
import eu.metatools.up.Shell
import eu.metatools.up.dt.Change
import eu.metatools.up.dt.Lx

abstract class SXEnt(shell: Shell, id: Lx, val sx: SX) : Ent(shell, id) {
    override fun partChanged(name: String, change: Change<*>) {
        if (change.isChange()) sx.updateUiRequested = true
    }

    override fun postConnect() {
        sx.updateUiRequested = true
    }

    override fun preDisconnect() {
        sx.updateUiRequested = true
    }
}