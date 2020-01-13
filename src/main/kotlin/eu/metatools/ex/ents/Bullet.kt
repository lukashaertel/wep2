package eu.metatools.ex.ents

import eu.metatools.ex.Frontend
import eu.metatools.ex.ents.Constants.tileHeight
import eu.metatools.ex.ents.Constants.tileWidth
import eu.metatools.f2d.data.Mat
import eu.metatools.f2d.data.Q
import eu.metatools.f2d.data.QPt
import eu.metatools.f2d.data.toQ
import eu.metatools.f2d.immediate.submit
import eu.metatools.f2d.tools.CaptureCube
import eu.metatools.up.Ent
import eu.metatools.up.Shell
import eu.metatools.up.dsl.provideDelegate
import eu.metatools.up.dt.Lx
import eu.metatools.up.dt.div
import eu.metatools.up.dt.lx

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
    initOwner: Mover,
    initPos: QPt, initVel: QPt, initMoveTime: Double, initLevel: Q, val damage: Int
) : Ent(shell, id), Moves, Solid, HandlesHit, Ticking, Rendered {
    companion object {
        /**
         * The drawable for the bullet.
         */
        private val drawable by atlas("arrow")
    }

    override val extraArgs = mapOf(
        "initOwner" to initOwner,
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

    val owner by { initOwner }
    /**
     * Current position.
     */
    override var pos by { initPos }

    /**
     * Current move time.
     */
    override var moveTime by { initMoveTime }

    val birth = initMoveTime

    /**
     * Current velocity.
     */
    override var vel by { initVel }

    override var level by { initLevel }

    /**
     * Constant. Radius.
     */
    override val radius = 0.05f.toQ()

    override fun render(mat: Mat, time: Double) {
        // Get position and height.
        val (x, y, z) = xyz(time)

        // Transformation for displaying the bullet.
        val local = mat
            .translate(x = tileWidth * x.toFloat(), y = tileHeight * y.toFloat())
            .translate(y = tileHeight * z.toFloat())
            .translate(y = tileHeight * Mover.offset.toFloat())
            .translate(z = toZ(z))
            .rotateZ(vel.angle.toFloat())
            .scale(tileWidth, tileHeight)

        // Submit the solid.
        ui.world.submit(drawable, time, local)
        ui.world.submit(CaptureCube, this, time, local)
    }

    override fun hitHull() {
        delete(this)
    }

    override fun hitOther(other: Moves) {
        (other as? Damageable)?.takeDamage(damage)?.let {
            owner.xp += it
        }

        delete(this)
    }

    override fun update(sec: Double, freq: Long) {
        // Delete if flying for too long.
        if (birth + 5.0 < sec)
            delete(this)
    }

}