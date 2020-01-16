package eu.metatools.ex.ents.items

import eu.metatools.ex.Frontend
import eu.metatools.ex.ents.*
import eu.metatools.ex.ents.Constants.tileHeight
import eu.metatools.ex.ents.Constants.tileWidth
import eu.metatools.ex.ents.hero.Hero
import eu.metatools.f2d.data.Mat
import eu.metatools.f2d.data.Q
import eu.metatools.f2d.data.QPt
import eu.metatools.f2d.drawable.Drawable
import eu.metatools.f2d.immediate.submit
import eu.metatools.f2d.tools.CaptureCube
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
    initPos: QPt, initHeight: Q
) : Ent(shell, id), Flying, Solid, Rendered, Damageable, Described {
    override val world get() = shell.resolve(lx / "root") as World

    override val radius = Q.THIRD

    override var xy by { initPos }

    override var z by { initHeight }

    override var t0 by { 0.0 }

    override var dXY by { QPt.ZERO }

    override var dZ by { Q.ZERO }

    protected abstract fun visual(): Drawable<Unit?>

    abstract fun apply(hero: Hero)

    override fun render(mat: Mat, time: Double) {
        // Get position and height.
        val (x, y, z) = xyzAt(time)

        // Transformation for displaying the res pack.
        val local = mat
            .translate(x = tileWidth * x.toFloat(), y = tileHeight * y.toFloat())
            .translate(y = tileHeight * z.toFloat())
            .translate(z = toZ(z))
            .scale(tileWidth, tileHeight)

        val visual = visual()

        // Submit the solid.
        ui.world.submit(visual, time, local)
        ui.world.submit(CaptureCube, this, time, local)
    }

    override fun takeDamage(amount: Q): Int {
        delete(this)
        return 1
    }
}