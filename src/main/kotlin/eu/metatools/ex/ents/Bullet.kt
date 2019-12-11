package eu.metatools.ex.ents

import eu.metatools.f2d.context.refer
import eu.metatools.ex.*
import eu.metatools.f2d.math.Mat
import eu.metatools.f2d.math.Pt
import eu.metatools.up.Ent
import eu.metatools.up.Shell
import eu.metatools.up.dsl.provideDelegate
import eu.metatools.up.dt.Lx
import eu.metatools.up.dt.div
import eu.metatools.up.dt.lx
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
    shell: Shell, id: Lx, initPos: Pt, initVel: Pt, initMoveTime: Double, val damage: Int
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

    /**
     * Constant. Radius.
     */
    override val radius = 0.025f

    override fun render(time: Double) {
        // Get time.
        val (x, y) = posAt(time)

        // Transformation for displaying the bullet.
        val mat = Mat.translation(Constants.tileWidth * x, Constants.tileHeight * y)
            .rotateZ(atan2(vel.y, vel.x))
            .scale(Constants.tileWidth * radius * 5f, Constants.tileHeight * radius * 2f)

        // Submit the solid.
        frontend.continuous.submit(solid, time, mat)
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