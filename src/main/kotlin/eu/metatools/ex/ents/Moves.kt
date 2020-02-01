package eu.metatools.ex.ents

import eu.metatools.ex.geom.depth
import eu.metatools.ex.geom.filterNormal
import eu.metatools.ex.geom.forEach
import eu.metatools.ex.math.sp
import eu.metatools.f2d.data.Tri
import eu.metatools.f2d.data.Vec
import eu.metatools.f2d.data.Vecs
import eu.metatools.f2d.data.isNotEmpty
import eu.metatools.up.isConnected
import eu.metatools.up.list
import kotlin.math.roundToInt

val g = 5f

fun World.bind(radius: Float, from: Vec, to: Vec): Vecs {
    // Get mesh path.
    val xa = minOf(from.x.roundToInt(), to.x.roundToInt())
    val xb = maxOf(from.x.roundToInt(), to.x.roundToInt())
    val ya = minOf(from.y.roundToInt(), to.y.roundToInt())
    val yb = maxOf(from.y.roundToInt(), to.y.roundToInt())
    val za = minOf(from.z.roundToInt(), to.z.roundToInt())
    val zb = maxOf(from.z.roundToInt(), to.z.roundToInt())

    // Compute dir.
    val dir = to - from

    // Initialize result vectors.
    var t = Float.MAX_VALUE
    var p = Vec.Zero
    var n = Vec.Zero

    // Iterate path.
    for (x in xa..xb) for (y in ya..yb) for (z in za..zb) {
        // Block has no mesh, return.
        val mesh = meshes[Tri(x, y, z)]
            ?: continue

        // Try to find better solution.
        mesh.forEach { v1, v2, v3 ->
            val (newT, newP) = sp(from, dir, v1, v2, v3, radius, t)
            if (newT < t) {
                t = newT
                p = newP
                n = (v3 - v1) cross (v2 - v1)
            }
        }
    }

    return if (t < Float.MAX_VALUE)
        Vecs(p, n)
    else
        Vecs(0)
}

/**
 * Entity moves.
 */
interface Moves : All {
    /**
     * Base position of the current movement.
     */
    var pos: Vec

    /**
     * Time at which movement began.
     */
    var t0: Double

    /**
     * Movement velocity.
     */
    var vel: Vec

    /**
     * True of the mover is on an non-sloped ground block.
     */
    var grounded: Boolean

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
    pos + vel * (time - t0).toFloat()

/**
 * Entity will block movement.
 */
interface Blocking : Moves

/**
 * Updates the velocity and sets the base values to evaluation at the given [time].
 */
fun Moves.takeMovement(time: Double, vel: Vec) {
    // Explicit update.
    pos = posAt(time)
    t0 = time
    this.vel = vel
}

/**
 * Gets the radius or zero if receiver does not have a radius.
 */
val Any.radius
    get() =
        if (this is Solid) radius else 0f

/**
 * [Moves] that handles hits.
 */
interface HandlesHit {
    /**
     * Entity hit the world hull.
     */
    fun hitHull(velPrime: Vec) = Unit

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

        a.pos = a.posAt(time)
        a.t0 = time

        // Add gravity if not flying.
        if (!a.flying && !a.grounded)
            a.vel -= Vec.Z * g * deltaTime.toFloat()

        // Collide with other.
        for (b in moves.subList(i + 1, moves.size)) {
            // Skip if deleted from other.
            if (!a.isConnected())
                continue
            if (!b.isConnected())
                continue

            // Don't collide if ignored.
            if (a.ignores(b) || b.ignores(a))
                continue

            val rel = b.posAt(time) - a.pos
            val penetration = a.radius + b.radius - rel.len
            if (penetration <= 0f)
                continue

            // Mark hit.
            if (a.isConnected() && b.isConnected() && a is HandlesHit) a.hitOther(b)
            if (a.isConnected() && b.isConnected() && b is HandlesHit) b.hitOther(a)

            if (rel.len == 0f)
                continue
            if (a !is Blocking || b !is Blocking)
                continue

            val resolve = rel.nor * penetration * 0.5f

            a.pos = a.pos - resolve
            b.pos = b.posAt(time) + resolve
        }

        // Skip if deleted after touch phase.
        if (!a.isConnected())
            continue

        // Get integral position to address block map.
        val triX = a.pos.x.roundToInt()
        val triY = a.pos.y.roundToInt()
        val triZ = a.pos.z.roundToInt()

        // Initialize out position and velocity.
        var outPos = a.pos
        var outVel = a.vel
        var outGrounded = false

        // Update on ground flag, set if a ground normal triangle hits the extended hull.
        for (x in triX.dec()..triX.inc()) for (y in triY.dec()..triY.inc()) for (z in triZ.dec()..triZ.inc())
            meshes[Tri(x, y, z)]?.filterNormal(Vec.Z, 0.7f)?.depth(outPos, a.radius + 0.1f)?.let {
                outGrounded = true
            }

        // Update collision.
        for (x in triX.dec()..triX.inc()) for (y in triY.dec()..triY.inc()) for (z in triZ.dec()..triZ.inc())
            meshes[Tri(x, y, z)]?.depth(outPos, a.radius)?.let { (n, d) ->
                outPos += n * d
                outVel -= n * (outVel dot n)
            }

        // TODO: Friction

        // Get velocity change.
        val velPrime = outVel - a.vel

        // Update values if changed.
        if (a.pos != outPos)
            a.pos = outPos
        if (a.vel != outVel)
            a.vel = outVel
        if (a.grounded != outGrounded)
            a.grounded = outGrounded

        // If collision occurred, and element handles hit, dispatch hit hull with velocity change.
        if (velPrime.isNotEmpty())
            (a as? HandlesHit)?.hitHull(velPrime)

    }
}