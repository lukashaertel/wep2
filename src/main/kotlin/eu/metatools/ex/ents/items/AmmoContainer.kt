package eu.metatools.ex.ents.items

import eu.metatools.ex.Frontend
import eu.metatools.ex.ents.Blocks
import eu.metatools.ex.ents.hero.Hero
import eu.metatools.f2d.data.Pt
import eu.metatools.f2d.data.Vec
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
class AmmoContainer(
    shell: Shell, id: Lx, ui: Frontend,
    initPos: Vec,  val content: Int
) : Container(shell, id, ui, initPos) {
    override val extraArgs = mapOf(
        "initPos" to initPos,
        "content" to content
    )

    override fun visual() = when {
        content > 12 -> Blocks.BigCrate.body
        content > 8 -> Blocks.Chest.body
        else -> Blocks.Crate.body
    } ?: never

    override fun apply(hero: Hero) {
        hero.takeAmmo(content)
    }

    override fun describe(): String = "$content arrows"
}