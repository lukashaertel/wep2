package eu.metatools.sx.ents

import eu.metatools.sx.SX
import eu.metatools.up.Ent
import eu.metatools.up.Shell
import eu.metatools.up.dsl.set
import eu.metatools.up.dt.Lx
import kotlinx.coroutines.runBlocking

class World(
    shell: Shell, id: Lx, val sx: SX
) : Ent(shell, id) {
    val players by set<Player>()

    companion object {
        /**
         * Milliseconds for update.
         */
        const val millis = 100L

        /**
         * Delta-time for the update.
         */
        val dt = millis / 1000f
    }

    /**
     * Periodic world update.
     */
    val worldUpdate = repeating(Short.MAX_VALUE, millis, shell::initializedTime) {
        runBlocking {

        }
    }

    /**
     * Renders all actors.
     */
    fun render(time: Double, delta: Double) {

    }
}