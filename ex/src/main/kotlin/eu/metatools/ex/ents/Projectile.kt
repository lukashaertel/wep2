package eu.metatools.ex.ents

import eu.metatools.ex.EX
import eu.metatools.ex.atlas
import eu.metatools.ex.ents.Constants.tileHeight
import eu.metatools.ex.ents.Constants.tileWidth
import eu.metatools.ex.ents.hero.Hero
import eu.metatools.fio.data.Mat
import eu.metatools.fio.data.Pt
import eu.metatools.fio.data.Vec
import eu.metatools.fio.immediate.submit
import eu.metatools.fio.tools.CaptureCube
import eu.metatools.up.Ent
import eu.metatools.up.Shell
import eu.metatools.up.delete
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
 * @param initXY The starting position of the projectile.
 * @param initZ The height of the projectile.
 * @param initDXY The planar velocity this projectile files with.
 * @param initDXY The vertical velocity this projectile files with.
 * @param initT0 The time the movement started (genesis of the projectile).
 * @property damage The damage this projectile does.
 */
class Projectile(
    shell: Shell,
    id: Lx,
    val ui: EX,
    initPos: Vec,
    initVel: Vec,
    initT0: Double,
    val damage: Float
) : Ent(shell, id), Moves, Solid, HandlesHit, Ticking, Rendered {
    companion object {
        /**
         * The drawable for the bullet.
         */
        val arrow by atlas("arrow")
    }

    override val extraArgs = mapOf(
        "initPos" to initPos,
        "initVel" to initVel,
        "initT0" to initT0,
        "damage" to damage
    )

    override val world get() = shell.resolve(lx / "root") as World

    override var pos by { initPos }

    override var vel by { initVel }

    override var t0 by { initT0 }

    override var grounded by { false }

    override val flying = true

    override val radius = 0.05f

    /**
     * The genesis of the projectile.
     */
    val genesis = initT0

    /**
     * The owner.
     */
    var owner by { null as Hero? }

    override fun ignores(other: Moves): Boolean {
        return other == owner
    }

    override fun render(mat: Mat, time: Double) {
        // Get position and height.
        val (x, y, z) = posAt(time)

        // Transformation for displaying the bullet.
        val local = mat
            .translate(x = tileWidth * x, y = tileHeight * y)
            .translate(y = tileHeight * z)
            .translate(z = toZ(y, z))
            .rotateZ((Pt(vel.x, vel.y).angle)) // TODO: Include dZ (project then calculate projected angle)
            .translate(x = -8f)
            .scale(tileWidth, tileHeight)

        // Submit the solid.
        ui.world.submit(arrow, time, local)
        ui.world.submit(CaptureCube, this, time, local)
    }


    override fun hitHull(velPrime: Vec) {
        // Hit the hull, just delete.
        delete()
    }

    override fun hitOther(other: Moves) {
        // Hit other, other is damageable.
        (other as? Damageable)?.takeDamage(damage)?.let {
            // If XP was returned, have owner receive it.
            owner?.takeXP(it)
        }

        // Delete.
        delete()
    }

    override fun update(sec: Double, freq: Long) {
        // Delete if flying for too long.
        if (genesis + 5.0 < sec)
            delete()
    }

}