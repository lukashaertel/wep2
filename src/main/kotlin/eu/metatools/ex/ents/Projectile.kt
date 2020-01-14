package eu.metatools.ex.ents

import eu.metatools.ex.Frontend
import eu.metatools.ex.ents.Constants.tileHeight
import eu.metatools.ex.ents.Constants.tileWidth
import eu.metatools.ex.ents.hero.Hero
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
 * A moving projectile. [Moves], is [Solid], [HandlesHit]s, [Ticking] until it has reached the auto-destruct, [Rendered]
 * as an arrow.
 *
 * @param shell The entity shell.
 * @param id The entity ID.
 * @property ui The displaying UI.
 * @param initOwner The [Hero] that owns this projectile for XP tracking.
 * @param initPos The starting position of the projectile.
 * @param initHeight The height of the projectile.
 * @param initMoveVel The velocity this projectile files with.
 * @param initMoveTime The time the movement started (genesis of the projectile).
 * @property damage The damage this projectile does.
 */
class Projectile(
    shell: Shell,
    id: Lx,
    val ui: Frontend,
    initOwner: Hero,
    initPos: QPt,
    initHeight: Q,
    initMoveVel: QPt,
    initMoveTime: Double,
    val damage: Q
) : Ent(shell, id), Moves, Solid, HandlesHit, Ticking, Rendered {
    companion object {
        /**
         * The drawable for the bullet.
         */
        val arrow by eu.metatools.ex.atlas("arrow")
    }

    override val extraArgs = mapOf(
        "initOwner" to initOwner,
        "initPos" to initPos,
        "initMoveVel" to initMoveVel,
        "initMoveTime" to initMoveTime,
        "initHeight" to initHeight,
        "damage" to damage
    )

    override val world get() = shell.resolve(lx / "root") as World

    override var pos by { initPos }

    override var height by { initHeight }

    override var moveTime by { initMoveTime }

    override var moveVel by { initMoveVel }

    override val radius = 0.05f.toQ()

    /**
     * The genesis of the projectile.
     */
    val genesis = initMoveTime

    /**
     * The owner.
     */
    val owner by { initOwner }

    override fun render(mat: Mat, time: Double) {
        // Get position and height.
        val (x, y, z) = xyz(time)

        // Transformation for displaying the bullet.
        val local = mat
            .translate(x = tileWidth * x.toFloat(), y = tileHeight * y.toFloat())
            .translate(y = tileHeight * z.toFloat())
            .translate(y = tileHeight * Hero.offset.toFloat())
            .translate(z = toZ(z))
            .rotateZ(moveVel.angle.toFloat())
            .scale(tileWidth, tileHeight)

        // Submit the solid.
        ui.world.submit(arrow, time, local)
        ui.world.submit(CaptureCube, this, time, local)
    }

    override fun hitHull() {
        // Hit the hull, just delete.
        delete(this)
    }

    override fun hitOther(other: Moves) {
        // Hit other, other is damageable.
        (other as? Damageable)?.takeDamage(damage)?.let {
            // If XP was returned, have owner receive it.
            owner.takeXP(it)
        }

        // Delete.
        delete(this)
    }

    override fun update(sec: Double, freq: Long) {
        // Delete if flying for too long.
        if (genesis + 5.0 < sec)
            delete(this)
    }

}