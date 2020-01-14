package eu.metatools.ex.ents

import eu.metatools.ex.Frontend
import eu.metatools.ex.ents.Constants.tileHeight
import eu.metatools.ex.ents.Constants.tileWidth
import eu.metatools.f2d.data.Mat
import eu.metatools.f2d.data.Q
import eu.metatools.f2d.data.QPt
import eu.metatools.f2d.immediate.submit
import eu.metatools.f2d.tools.CaptureCube
import eu.metatools.up.Ent
import eu.metatools.up.Shell
import eu.metatools.up.dsl.provideDelegate
import eu.metatools.up.dt.Lx
import eu.metatools.up.dt.div
import eu.metatools.up.dt.lx
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
    shell: Shell, id: Lx, val ui: Frontend,
    initPos: QPt, initHeight: Q, val content: Int
) : Ent(shell, id), Moves, Solid, Rendered, Damageable, Described {
    override val extraArgs = mapOf(
        "initPos" to initPos,
        "initHeight" to initHeight,
        "content" to content
    )

    override val world get() = shell.resolve(lx / "root") as World

    override val radius = Q.THIRD

    override var pos by { initPos }

    override var height by { initHeight }

    override var moveTime by { 0.0 }

    override var moveVel by { QPt.ZERO }

    override fun render(mat: Mat, time: Double) {
        // Get position and height.
        val (x, y, z) = xyz(time)

        // Transformation for displaying the res pack.
        val local = mat
            .translate(x = tileWidth * x.toFloat(), y = tileHeight * y.toFloat())
            .translate(y = tileHeight * z.toFloat())
            .translate(z = toZ(z))
            .scale(tileWidth, tileHeight)

        val visual = when {
            content > 12 -> Blocks.BigCrate.body
            content > 8 -> Blocks.Chest.body
            else -> Blocks.Crate.body
        } ?: never

        // Submit the solid.
        ui.world.submit(visual, time, local)
        ui.world.submit(CaptureCube, this, time, local)
    }

    override fun takeDamage(amount: Q): Int {
        delete(this)
        return 1
    }

    override fun describe(): String = "$content arrows"
}