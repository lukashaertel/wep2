package eu.metatools.ex.ents

import com.badlogic.gdx.graphics.Color
import eu.metatools.f2d.context.refer
import eu.metatools.ex.*
import eu.metatools.f2d.math.Mat
import eu.metatools.f2d.math.RealPt
import eu.metatools.f2d.math.toReal
import eu.metatools.f2d.tools.Cube
import eu.metatools.f2d.tools.tint
import eu.metatools.up.Ent
import eu.metatools.up.Shell
import eu.metatools.up.dsl.provideDelegate
import eu.metatools.up.dt.*

class Respack(
    shell: Shell, id: Lx, val ui: Frontend,
    initPos: RealPt, val content: Int
) : Ent(shell, id), TraitMove, Ticking, Rendered, TraitDamageable, HasDescription {
    companion object {
        /**
         * The drawable for the bullet.
         */
        private val solid by lazy { Resources.solid.refer() }
    }

    override val extraArgs = mapOf(
        "initPos" to initPos,
        "content" to content
    )

    /**
     * Reference to the world.
     */
    override val world get() = shell.resolve(lx / "root") as World

    /**
     * Current position.
     */
    override var pos by { initPos }

    /**
     * Current move time.
     */
    override var moveTime by { 0.0 }

    /**
     * Current velocity.
     */
    override var vel by { RealPt.ZERO }

    /**
     * Constant. Radius.
     */
    override val radius = 0.15f.toReal()

    override val blocking get() = false

    override fun render(time: Double) {
        // Get time.
        val (x, y) = posAt(time)

        // Transformation for displaying the bullet.
        val mat = Mat.translation(Constants.tileWidth * x.toFloat(), Constants.tileHeight * y.toFloat())
            .rotateZ(time.toFloat())
            .scale(Constants.tileWidth * radius.toFloat() * 2f, Constants.tileHeight * radius.toFloat() * 2f)

        // Get color.
        val activeColor = if (ui.isSelected(this)) Color.WHITE else Color.GRAY

        // Submit the solid.
        ui.submit(solid.tint(activeColor), time, mat)
        ui.submit(Cube, this, time, mat)
    }


    override fun update(sec: Double, freq: Long) {
        // Update movement, capture all hit objects.
        val hit = updateMove(sec, freq)

        // Not empty, this bullet is invalid as of now.
        if (hit.isNotEmpty())
            delete(this)

        for (it in hit) {
            if (it is TraitCollects) {
                it.collectResource(content)
                return
            }
        }
    }

    override fun takeDamage(amount: Int) {
        delete(this)
    }

    override val describe: String
        get() = "Lädierter Schädel" /// "$content resources"


}