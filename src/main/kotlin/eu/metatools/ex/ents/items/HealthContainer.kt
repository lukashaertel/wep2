package eu.metatools.ex.ents.items

import eu.metatools.ex.Frontend
import eu.metatools.ex.ents.Blocks
import eu.metatools.ex.ents.hero.Hero
import eu.metatools.f2d.data.Q
import eu.metatools.f2d.data.QPt
import eu.metatools.up.Shell
import eu.metatools.up.dt.Lx
import eu.metatools.up.lang.never

/**
 * Ammo container.
 *
 * @param shell The entity shell.
 * @param id The entity ID.
 * @property ui The displaying UI.
 * @param initPos The starting position of the container.
 * @param initHeight The height of the container.
 */
class HealthContainer(
    shell: Shell, id: Lx, ui: Frontend,
    initPos: QPt, initHeight: Q, val content: Q
) : Container(shell, id, ui, initPos, initHeight) {
    override val extraArgs = mapOf(
        "initPos" to initPos,
        "initHeight" to initHeight,
        "content" to content
    )

    override fun visual() = Blocks.Barrel.body ?: never

    override fun apply(hero: Hero) {
        hero.takeHealth(content)
    }

    override fun describe(): String = "$content health"
}