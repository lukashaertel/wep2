package eu.metatools.ex.ents

import eu.metatools.f2d.data.Q
import eu.metatools.f2d.data.QPt
import eu.metatools.f2d.data.toQ
import eu.metatools.up.isConnected
import eu.metatools.up.lang.invoke
import eu.metatools.up.list

interface HasDescription {
    val describe: String
}


interface Solid : All {
    val radius: Q
}

interface Moves : All {
    var pos: QPt

    var moveTime: Double

    var vel: QPt

    var level: Q
}

fun Moves.positionAt(time: Double) =
    if (vel.isEmpty()) pos else pos + vel * (time - moveTime).toQ()

interface Walking : Moves

interface Blocking : Moves

fun Moves.move(time: Double, input: QPt) {
    // Explicit update.
    pos = positionAt(time)
    moveTime = time
    vel = input
}

fun Any.radius() =
    if (this is Solid) radius else Q.ZERO

interface HandlesHit {
    fun hitHull() = Unit
    fun hitBoundary() = Unit
    fun hitOther(other: Moves) = Unit
}

fun World.updateMovement(time: Double) {
    val moves = shell.list<Moves>().toList()

    for ((i, a) in moves.withIndex()) {
        // Skip if deleted from other.
        if (!a.isConnected())
            continue

        // Base update.
        a.pos = a.positionAt(time)
        a.moveTime = time

        for (b in moves.subList(i + 1, moves.size)) {
            // Skip if deleted from other.
            if (!a.isConnected())
                continue
            if (!b.isConnected())
                continue

            val aLevel = a.level.toInt()
            val bLevel = b.level.toInt()

            val aPos = a.positionAt(time)
            val bPos = b.positionAt(time)
            val aRadius = a.radius()
            val bRadius = b.radius()

            val aIntermediate = map.intermediate(aLevel, aPos.x, aPos.y)
            val bIntermediate = map.intermediate(bLevel, bPos.x, bPos.y)

            // Skip if they cannot touch.
            if (aLevel != bLevel
                && (!aIntermediate || aLevel.dec() != bLevel)
                && (!bIntermediate || bLevel.dec() != aLevel)
            ) continue

            val rel = bPos - aPos
            val penetration = aRadius + bRadius - rel.len
            if (penetration <= Q.ZERO)
                continue

            // Mark hit.
            if (a.isConnected() && b.isConnected() && a is HandlesHit) a.hitOther(b)
            if (a.isConnected() && b.isConnected() && b is HandlesHit) b.hitOther(a)

            if (rel.isEmpty())
                continue
            if (a !is Blocking)
                continue
            if (b !is Blocking)
                continue

            val resolve = rel.nor * penetration / Q.TWO
            a.pos = aPos - resolve
            b.pos = bPos + resolve
        }

        // Skip if deleted after touch phase.
        if (!a.isConnected())
            continue

        // Evaluate for hull collision.
        val hullLevel = a.level.toInt()
        val hullPos = a.positionAt(time)
        val hullRadius = a.radius()

        // Evaluate the intermediate state (if evaluating two levels instead of one).
        val hullIntermediate = map.intermediate(hullLevel, hullPos.x, hullPos.y)

        // Bind in hull.
        val hullBindFst = hull.bindOut(hullLevel, hullRadius, hullPos.x, hullPos.y)
        val hullBindSnd = hullIntermediate { hull.bindOut(hullLevel.inc(), hullRadius, hullPos.x, hullPos.y) }
        val hullBind = hullBindFst ?: hullBindSnd
        if (hullBind != null) {
            a.pos = QPt(hullBind.first, hullBind.second)
            (a as? HandlesHit)?.hitHull()
        }

        // Not walking, done here.
        if (a !is Walking)
            continue

        // Skip if deleted after hit hull phase.
        if (!a.isConnected())
            continue

        // Evaluate for boundary collision.
        val boundsLevel = a.level.toInt()
        val boundsPos = a.positionAt(time)
        val boundsRadius = a.radius()

        // Evaluate the intermediate state again.
        val boundsIntermediate = map.intermediate(boundsLevel, boundsPos.x, boundsPos.y)

        // Bind in boundary.
        val boundsBindFst = bounds.bindIn(boundsLevel, boundsRadius, boundsPos.x, boundsPos.y)
        val boundsBindSnd =
            boundsIntermediate { bounds.bindIn(boundsLevel.inc(), boundsRadius, boundsPos.x, boundsPos.y) }
        val boundsBind = boundsBindFst ?: boundsBindSnd
        if (boundsBind != null) {
            a.pos = QPt(boundsBind.first, boundsBind.second)
            (a as? HandlesHit)?.hitBoundary()
        }

        // Skip if deleted after hit bounds phase.
        if (!a.isConnected())
            continue

        // Evaluate for lift detection.
        val liftLevel = a.level.toInt()
        val liftPos = a.positionAt(time)

        // Lift if in lifting area.
        val lift = map.lift(liftLevel, liftPos.x, liftPos.y)
        if (lift != 0)
            a.level = (a.level.floor() + lift).toQ()
    }
}

fun Moves.xyz(time: Double): Triple<Q, Q, Number> {
    // Get position.
    val (x, y) = positionAt(time)

    // Get height, if walking, this is clipped to ground.
    val z = if (this is Walking)
        world.map.height(level.toInt(), x, y)
    else
        level

    // Return the values.
    return Triple(x, y, z)
}