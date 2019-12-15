package eu.metatools.ex.ents

import eu.metatools.ex.math.root
import eu.metatools.up.Ent
import eu.metatools.f2d.math.Real
import eu.metatools.f2d.math.RealPt
import eu.metatools.f2d.math.toPt
import eu.metatools.f2d.math.toReal

/**
 * Entity has a radius.
 */
interface TraitRadius {
    /**
     * The radius of the entity.
     */
    val radius: Real
}

/**
 * Entity refers to the world.
 */
interface TraitWorld {
    /**
     * The world.
     */
    val world: World
}

/**
 * Entity can take damage.
 */
interface TraitDamageable {
    /**
     * Takes that damage.
     * @param amount The amount of damage to take.
     */
    fun takeDamage(amount: Int)
}

/**
 * Entity moves, must have a world and a radius reference as well.
 */
interface TraitMove : TraitWorld, TraitRadius {
    /**
     * The base position.
     */
    var pos: RealPt

    /**
     * The movement time.
     */
    var moveTime: Double

    /**
     * The velocity.
     */
    var vel: RealPt

    /**
     * Determines the actual position at the time.
     */
    fun posAt(time: Double): RealPt {
        if (vel.isEmpty)
            return pos

        val dt = (time - moveTime).toReal()
        return pos + vel * dt
    }

    /**
     * Receives a new movement.
     */
    fun receiveMove(sec: Double, vel: RealPt) {
        pos = posAt(sec)
        moveTime = sec
        this.vel = vel
    }

    /**
     * Updats the movement, returning a list of touched entities.
     */
    fun updateMove(sec: Double, freq: Long): List<Ent> {
        // No velocity, so nothing to do.
        if (vel.isEmpty)
            return emptyList()

        // Get base parameters.
        pos = posAt(sec)
        moveTime = sec

        // Make result set.
        val hit = mutableListOf<Ent>()

        // Get SDF for own radius, check if hitting. If so, un-clip and add world to result set.
        val sdf = world.sdf(radius)
        val distance = sdf(pos)
        if (distance < Real.Zero) {
            val clip = root(sdf, pos)
            pos = (clip + clip) - pos
            hit += world
        }

        // Check all other movers, move away if clipping and add to result set.
        for (other in world.movers) {
            // Just as an example, not good code.
            if (other === this)
                continue
            val d = other.pos - pos
            if (d.len == Real.Zero)
                continue
            val rs = radius + other.radius
            if (d.len < rs) {
                val pen = rs - d.len
                pos -= d.nor * pen
                hit += other
            }
        }

        return hit
    }
}
