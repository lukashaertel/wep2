package eu.metatools.ex.ents

import com.badlogic.gdx.graphics.Color
import eu.metatools.f2d.context.refer
import eu.metatools.ex.*
import eu.metatools.f2d.math.Mat
import eu.metatools.f2d.math.RealPt
import eu.metatools.f2d.math.toReal
import eu.metatools.f2d.tools.tint
import eu.metatools.up.Ent
import eu.metatools.up.Shell
import eu.metatools.up.dsl.provideDelegate
import eu.metatools.up.dt.*
import kotlin.math.atan2

class Respack(
    shell: Shell, id: Lx, initPos: RealPt, val content: Int
) : Ent(shell, id), TraitMove, Ticking, Rendered, TraitDamageable {
    companion object {
        /**
         * The drawable for the bullet.
         */
        private val solid by lazy { Resources.solid.refer().tint(Color.CYAN) }
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
    override var vel by { RealPt.Zero }

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

        // Submit the solid.
        frontend.continuous.submit(solid, time, mat)
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

}