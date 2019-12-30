package eu.metatools.ex.ents

import eu.metatools.f2d.resource.refer
import eu.metatools.ex.*
import eu.metatools.f2d.data.Mat
import eu.metatools.f2d.data.RealPt
import eu.metatools.f2d.data.toReal
import eu.metatools.f2d.immediate.submit
import eu.metatools.f2d.tools.Cube
import eu.metatools.up.Ent
import eu.metatools.up.Shell
import eu.metatools.up.dsl.provideDelegate
import eu.metatools.up.dt.*
import kotlin.math.atan2

/**
 * A moving bullet.
 * @param shell The shell for the [Ent].
 * @param id The ID for the [Ent].
 * @param initPos The initial position.
 * @param initVel The initial velocity.
 * @param initMoveTime The initial move time.
 * @property damage Constant. The damage done by the bullet.
 */
class Bullet(
    shell: Shell, id: Lx, val ui: Frontend,
    initPos: RealPt, initVel: RealPt, initMoveTime: Double, initLevel: Int, val damage: Int
) : Ent(shell, id), TraitMove,
    Ticking, Rendered {
    companion object {
        /**
         * The drawable for the bullet.
         */
        private val solid by lazy { Resources.solid.refer() }
    }

    override val extraArgs = mapOf(
        "initPos" to initPos,
        "initVel" to initVel,
        "initMoveTime" to initMoveTime,
        "initLevel" to initLevel,
        "damage" to damage
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
    override var moveTime by { initMoveTime }

    /**
     * Current velocity.
     */
    override var vel by { initVel }

    override var level by { initLevel }

    /**
     * Constant. Radius.
     */
    override val radius = 0.025f.toReal()

    override val blocking get() = true

    override val clips = false
    override fun render(mat: Mat, time: Double) {
        // Get time.
        val (x, y) = posAt(time)

        // Transformation for displaying the bullet.
        val mat2 = mat * Mat.translation(
            Constants.tileWidth * x.toFloat(), Constants.tileHeight * y.toFloat(),
            -level.toFloat()
        )
            .rotateZ(atan2(vel.y.toFloat(), vel.x.toFloat()))
            .scale(Constants.tileWidth * radius.toFloat() * 5f, Constants.tileHeight * radius.toFloat() * 2f)

        // Submit the solid.
        ui.world.submit(solid, time, mat2)
        ui.world.submit(Cube, this, time, mat2)
    }


    override fun update(sec: Double, freq: Long) {
        // Update movement, capture all hit objects.
        val hit = updateMove(sec, freq)

        // Not empty, this bullet is invalid as of now.
        if (hit.isNotEmpty())
            delete(this)

        // For all items that are hit, dispatch the damage.
        hit.forEach {
            if (it is TraitDamageable)
                it.takeDamage(damage)
        }
    }

}