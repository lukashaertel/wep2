package eu.metatools.ex.ents

import eu.metatools.f2d.data.Q
import eu.metatools.f2d.data.QPt
import eu.metatools.f2d.data.QVec
import eu.metatools.f2d.data.toQ
import eu.metatools.up.isConnected
import eu.metatools.up.lang.invoke
import eu.metatools.up.list

/**
 * Entity moves.
 */
interface Moves : All {
    /**
     * Base position of the current movement.
     */
    var xy: QPt

    /**
     * Time at which movement began.
     */
    var t0: Double

    /**
     * Movement velocity in the plane.
     */
    var dXY: QPt

    /**
     * True if this mover ignores collision with [other].
     */
    fun ignores(other: Moves) = false
}

/**
 * Entity moves and is clipped to the ground and the walkable patch.
 */
interface Walking : Moves {
    /**
     * Map layer to evaluate.
     */
    var layer: Int

    val elevation: Q get() = Q.HALF
}

interface Flying : Moves {
    /**
     * Height of the object.
     */
    var z: Q

    /**
     * Height change rate.
     */
    var dZ: Q
}

/**
 * Evaluates the planar coordinates of [Moves] ([Walking] or [Flying]).
 */
fun Moves.xyAt(time: Double) =
    if (dXY.isEmpty()) xy else xy + dXY * (time - t0).toQ()

/**
 * Evaluates the vertical coordinates of a [Walking].
 */
fun Walking.zAt(x: Q, y: Q) =
    world.map.height(layer, x, y) + elevation

/**
 * Evaluates the vertical coordinates of a [Flying].
 */
fun Flying.zAt(time: Double) =
    if (dZ == Q.ZERO) z else z + dZ * (time - t0).toQ()

/**
 * Evaluates the vertical coordinates of a [Walking].
 */
fun Walking.xyzAt(time: Double): QVec {
    val xy = xyAt(time)
    return QVec(xy.x, xy.y, zAt(xy.x, xy.y))
}

/**
 * Evaluates the vertical coordinates of a [Flying].
 */
fun Flying.xyzAt(time: Double): QVec {
    val xy = xyAt(time)
    return QVec(xy.x, xy.y, zAt(time))
}

/**
 * Evaluates the vertical coordinates of a [Moves] by type disambiguation.
 */
fun Moves.xyzAt(time: Double) = when (this) {
    is Walking -> xyzAt(time)
    is Flying -> xyzAt(time)
    else -> throw IllegalArgumentException("Unknown type: $this")
}

/**
 * Entity will block movement.
 */
interface Blocking : Moves

/**
 * Updates the velocity and sets the base values to evaluation at the given [time].
 */
fun Walking.takeMovement(time: Double, dXY: QPt) {
    // Explicit update.
    xy = xyAt(time)
    t0 = time
    this.dXY = dXY
}

/**
 * Updates the velocity and sets the base values to evaluation at the given [time].
 */
fun Flying.takeMovement(time: Double, dXY: QPt, dZ: Q) {
    // Explicit update.
    xy = xyAt(time)
    z = zAt(time)
    t0 = time
    this.dXY = dXY
    this.dZ = dZ
}

/**
 * Updates the velocity and sets the base values to evaluation at the given [time].
 */
fun Moves.takeMovement(time: Double, dXY: QPt, dZ: Q) = when (this) {
    is Walking -> takeMovement(time, dXY)
    is Flying -> takeMovement(time, dXY, dZ)
    else -> throw IllegalArgumentException("Unknown type: $this")
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
    fun hitHull() = Unit

    /**
     * Entity hit the walkable hull.
     */
    fun hitBoundary() = Unit

    /**
     * Entity hit [other] [Moves].
     */
    fun hitOther(other: Moves) = Unit
}

/**
 * Applies collision resolution.
 */
private fun resolve(on: Moves, resolution: QVec) {
    when (on) {
        is Walking -> {
            on.xy = QPt(resolution.x, resolution.y)
            on.layer = (resolution.z - on.elevation).floor()
        }
        is Flying -> {
            on.xy = QPt(resolution.x, resolution.y)
            on.z = resolution.z
        }
    }
}

/**
 * Updates all [Moves] in the world.
 */
fun World.updateMovement(time: Double) {

    val moves = shell.list<Moves>().toList()

    for ((i, a) in moves.withIndex()) {
        // Skip if deleted from other.
        if (!a.isConnected())
            continue

        // Perform base movement update.
        when (a) {
            is Walking -> {
                // Transfer intermediate coordinates.
                val (x, y, z) = a.xyzAt(time)
                a.xy = QPt(x, y)
                a.layer = (z - a.elevation).floor()
                a.t0 = time
            }
            is Flying -> {
                // Transfer intermediate coordinates.
                val (x, y, z) = a.xyzAt(time)
                a.xy = QPt(x, y)
                a.z = z
                a.t0 = time
            }
        }

        for (b in moves.subList(i + 1, moves.size)) {
            // Skip if deleted from other.
            if (!a.isConnected())
                continue
            if (!b.isConnected())
                continue

            // Don't collide if ignored.
            if (a.ignores(b) || b.ignores(a))
                continue

            val aPos = a.xyzAt(time)
            val bPos = b.xyzAt(time)
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

            resolve(a, aPos - resolve)
            resolve(b, bPos + resolve)
        }

        // Skip if deleted after touch phase.
        if (!a.isConnected())
            continue

        // Evaluate for hull collision.
        val hullPos = a.xyzAt(time)
        val hullLevel = hullPos.z.floor()
        val hullRadius = a.radius()

        // Evaluate the intermediate state (if evaluating two levels instead of one).
        val hullIntermediate = map.intermediate(hullLevel, hullPos.x, hullPos.y)

        // Bind in hull.
        val hullBindFst = hull.bindOut(hullLevel, hullRadius, hullPos.x, hullPos.y)
        val hullBindSnd = hullIntermediate { hull.bindOut(hullLevel.inc(), hullRadius, hullPos.x, hullPos.y) }
        val hullBind = hullBindFst ?: hullBindSnd
        if (hullBind != null) {
            // TODO: Other ways to bind (this is not really that thought through TBH).
            a.xy = QPt(hullBind.first, hullBind.second)
            (a as? HandlesHit)?.hitHull()
        }

        // Not walking, done here.
        if (a !is Walking)
            continue

        // Skip if deleted after hit hull phase.
        if (!a.isConnected())
            continue

        // Evaluate for boundary collision.
        val boundsPos = a.xyzAt(time)
        val boundsLevel = boundsPos.z.floor()
        val boundsRadius = a.radius()

        // Evaluate the intermediate state again.
        val boundsIntermediate = map.intermediate(boundsLevel, boundsPos.x, boundsPos.y)

        // Bind in boundary.
        val boundsBindFst = bounds.bindIn(boundsLevel, boundsRadius, boundsPos.x, boundsPos.y)
        val boundsBindSnd =
            boundsIntermediate { bounds.bindIn(boundsLevel.inc(), boundsRadius, boundsPos.x, boundsPos.y) }
        val boundsBind = boundsBindFst ?: boundsBindSnd
        if (boundsBind != null) {
            a.xy = QPt(boundsBind.first, boundsBind.second)
            (a as? HandlesHit)?.hitBoundary()
        }

        // Skip if deleted after hit bounds phase.
        if (!a.isConnected())
            continue

        // Evaluate for lift detection.
        val liftLevel = a.layer
        val liftPos = a.xyAt(time)

        // Lift if in lifting area.
        val lift = map.lift(liftLevel, liftPos.x, liftPos.y)
        if (lift != 0)
            a.layer += lift
    }
}