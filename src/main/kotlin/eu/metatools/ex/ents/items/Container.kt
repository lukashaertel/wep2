package eu.metatools.ex.ents.items

import eu.metatools.ex.Frontend
import eu.metatools.ex.ents.*
import eu.metatools.ex.ents.Constants.tileHeight
import eu.metatools.ex.ents.Constants.tileWidth
import eu.metatools.ex.ents.hero.Hero
import eu.metatools.fio.data.Mat
import eu.metatools.fio.data.Vec
import eu.metatools.fio.drawable.Drawable
import eu.metatools.fio.immediate.submit
import eu.metatools.fio.tools.CaptureCube
import eu.metatools.up.Ent
import eu.metatools.up.Shell
import eu.metatools.up.dsl.provideDelegate
import eu.metatools.up.dt.Lx
import eu.metatools.up.dt.div
import eu.metatools.up.dt.lx

/**
 * Base container.
 *
 * @param shell The entity shell.
 * @param id The entity ID.
 * @property ui The displaying UI.
 * @param initPos The starting position of the container.
 * @param initHeight The height of the container.
 */
abstract class Container(
    shell: Shell, id: Lx, val ui: Frontend,
    initPos: Vec
) : Ent(shell, id), Moves, Solid, Rendered, Damageable, Described {
    override val world get() = shell.resolve(lx / "root") as World

    override val radius = 0.3f

    override var pos by { initPos }

    override var vel by { Vec.Zero }

    override var t0 by { 0.0 }

    override var grounded by { false }

    protected abstract fun visual(): Drawable<Unit?>

    abstract fun apply(hero: Hero)

    override fun render(mat: Mat, time: Double) {
        // Get position and height.
        val (x, y, z) = posAt(time)

        // Transformation for displaying the res pack.
        val local = mat
            .translate(x = tileWidth * x, y = tileHeight * y)
            .translate(y = tileHeight * z)
            .translate(z = toZ(y, z))
            .scale(tileWidth, tileHeight)

        val visual = visual()

        // Submit the solid.
        ui.world.submit(visual, time, local)
        ui.world.submit(CaptureCube, this, time, local)
    }

    override fun takeDamage(amount: Float): Int {
        delete(this)
        return 1
    }
}
