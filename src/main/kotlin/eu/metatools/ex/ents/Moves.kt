package eu.metatools.ex.ents

import eu.metatools.f2d.data.Q
import eu.metatools.f2d.data.QVec
import eu.metatools.f2d.data.toQ
import eu.metatools.up.isConnected
import eu.metatools.up.list

val g = Q(10)

const val maxHullResolutions = 3

/**
 * Entity moves.
 */
interface Moves : All {
    /**
     * Base position of the current movement.
     */
    var pos: QVec

    /**
     * Time at which movement began.
     */
    var t0: Double

    /**
     * Movement velocity.
     */
    var vel: QVec

    /**
     * True if unaffected by gravity.
     */
    val flying: Boolean get() = false

    /**
     * True if this mover ignores collision with [other].
     */
    fun ignores(other: Moves) = false
}

/**
 * Evaluates the coordinates of [Moves].
 */
fun Moves.posAt(time: Double) =
    pos + vel * (time - t0).toQ()

/**
 * Entity will block movement.
 */
interface Blocking : Moves

/**
 * Updates the velocity and sets the base values to evaluation at the given [time].
 */
fun Moves.takeMovement(time: Double, vel: QVec) {
    // Explicit update.
    pos = posAt(time)
    t0 = time
    this.vel = vel
}

/**
 * Gets the radius or zero if receiver does not have a radius.
 */
fun Any.radius() =
    if (this is Solid) radius else Q.ZERO

/**
 * [Moves] that handles hits.
 */
interface HandlesHit {
    /**
     * Entity hit the world hull.
     */
    fun hitHull(velocity: QVec) = Unit

    /**
     * Entity hit the walkable hull.
     */
    fun hitGround(velocity: QVec) = Unit

    /**
     * Entity hit [other] [Moves].
     */
    fun hitOther(other: Moves) = Unit
}

/**
 * Updates all [Moves] in the world.
 */
fun World.updateMovement(time: Double, deltaTime: Double) {
    val moves = shell.list<Moves>().toList()

    for ((i, a) in moves.withIndex()) {
        // Skip if deleted from other.
        if (!a.isConnected())
            continue

        // Perform base movement update.
        a.pos = a.posAt(time)
        a.t0 = time

        // Add gravity if not flying.
        if (!a.flying)
            a.vel = a.vel - QVec.Z * (g * deltaTime) // TODO: Block off if on ground.

        for (b in moves.subList(i + 1, moves.size)) {
            // Skip if deleted from other.
            if (!a.isConnected())
                continue
            if (!b.isConnected())
                continue

            // Don't collide if ignored.
            if (a.ignores(b) || b.ignores(a))
                continue

            val aPos = a.posAt(time)
            val bPos = b.posAt(time)
            val aRadius = a.radius()
            val bRadius = b.radius()

            val rel = bPos - aPos
            val penetration = aRadius + bRadius - rel.len
            if (penetration <= Q.ZERO)
                continue

            // Mark hit.
            if (a.isConnected() && b.isConnected() && a is HandlesHit) a.hitOther(b)
            if (a.isConnected() && b.isConnected() && b is HandlesHit) b.hitOther(a)

            if (rel.len == Q.ZERO)
                continue
            if (a !is Blocking || b !is Blocking)
                continue

            val resolve = rel.nor * penetration / Q.TWO

            a.pos = aPos - resolve
            b.pos = bPos + resolve
        }

        // Skip if deleted after touch phase.
        if (!a.isConnected())
            continue

        // Perform hull collision resolution.
        repeat(maxHullResolutions) {
            // Not connected after one collision.
            if (!a.isConnected())
                return@repeat

            // Evaluate for hull collision.
//            val pos = a.posAt(time)
            val radius = a.radius()

            map.bindHeight(a.pos)?.let {
                if (a.pos.z < it) {
                    val vel = a.vel
                    a.pos = a.pos.copy(z = it)
                    a.vel = a.vel.copy(z = Q.ZERO)
                    (a as? HandlesHit)?.hitGround(vel)
                }
            }

            if (!a.isConnected())
                return@repeat

            // TODO: Bind validation on height. vv this is a shit-fix vv
            hull.bind(radius, a.pos.copy(z = a.pos.z.ceiling().toQ()))?.let {
                val vel = a.vel
                a.pos = a.pos.copy(x = it.x, y = it.y)
                a.vel = a.vel
                (a as? HandlesHit)?.hitHull(vel)
            }
        }
    }
}