package eu.metatools.ex.ents

import eu.metatools.up.Ent
import eu.metatools.f2d.math.Real
import eu.metatools.f2d.math.RealPt
import eu.metatools.f2d.math.toReal
import eu.metatools.up.list
import java.util.*

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

interface HasDescription {
    val describe: String
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

interface TraitCollects {
    fun collectResource(amount: Int)
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

    val blocking: Boolean

    var level: Int

    /**
     * If true, mover clips on both solid hull and clip hints.
     */
    val clips: Boolean get() = true

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
    fun updateMove(sec: Double, freq: Long): SortedSet<Ent> {
        // Make result set.
        val hit = TreeSet<Ent>()

        // Get base parameters.
        pos = posAt(sec)
        moveTime = sec

        // Set level to single entry key.
        if (clips)
            world.entries.entries.singleOrNull {
                it.value.contains(radius, pos)
            }?.let {
                level = it.key
            }

        // Get SDF for own radius, check if hitting. If so, un-clip and add world to result set.
        val dt = world.evaluateCollision(clips, level, radius, pos)
        if (dt.inside) {
            pos = dt.support + (dt.support - pos) * radius
            hit += world

            // TODO: Update velocity maybe?
        }

        // Check all other movers, move away if clipping and add to result set.
        for (other in world.shell.list<TraitMove>()) {
            // Just as an example, not good code.
            if (other === this)
                continue
            if (other.level != level)
                continue

            val d = other.pos - pos
            val rs = radius + other.radius
            if (d.len <= rs) {
                if (blocking && other.blocking && d.len != Real.ZERO) {
                    val pen = rs - d.len
                    pos -= d.nor * pen
                }
                hit += other as Ent
            }
        }

        return hit
    }
}
