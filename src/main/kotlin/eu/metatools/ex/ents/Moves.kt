package eu.metatools.ex.ents

import eu.metatools.ex.data.closest
import eu.metatools.ex.data.forEach
import eu.metatools.ex.data.inside
import eu.metatools.ex.math.sp
import eu.metatools.f2d.data.*
import eu.metatools.up.isConnected
import eu.metatools.up.list
import kotlin.math.roundToInt

val g = Vec(0f, 0f, -1f)

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

data class Dyn(val origin: Vec, val vel: Vec, val time: Double, val limit: Double) {
    constructor(position: Vec) : this(position, Vec.Zero, Double.NaN, Double.NaN)

    fun at(t: Double): Vec {
        if (vel.isEmpty())
            return origin
        val dt = minOf(limit, t) - time
        return Vec(
            (origin.x + vel.x * dt).toFloat(),
            (origin.y + vel.y * dt).toFloat(),
            (origin.z + vel.z * dt).toFloat()
        )
    }
}

// TODO: Simple and stupid push-out.
//       For fast movers, apply penetration check via. spheroid-passage

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
    fun hitHull(vel: Vec) = Unit

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
    val origins = moves.associateWith { it.pos }

    for ((i, a) in moves.withIndex()) {
        // Skip if deleted from other.
        if (!a.isConnected())
            continue

        a.pos = a.posAt(time)
        a.t0 = time

        // Add gravity if not flying.
        if (!a.flying)
            a.vel += g * deltaTime.toFloat() // TODO: Block off if on ground.

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

        val triX = a.pos.x.roundToInt()
        val triY = a.pos.y.roundToInt()
        val triZ = a.pos.z.roundToInt()
        for (x in triX.dec()..triX.inc()) for (y in triY.dec()..triY.inc()) for (z in triZ.dec()..triZ.inc()) {
            val mesh = meshes[Tri(x, y, z)] ?: continue

            if (!mesh.inside(a.pos, a.radius))
                continue

            val (tri, distance) = mesh.closest(a.pos)
            val t = (tri[1] - tri[0]).nor
            val b = (tri[2] - tri[0]).nor
            val n = (b cross t).nor

            // This is still fucked.
            val btn = Mat(
                b.x, b.y, b.z, 0f,
                b.x, t.y, t.z, 0f,
                b.x, n.y, n.z, 0f,
                0f, 0f, 0f, 1f
            )
            a.pos += n * (a.radius - distance)
            a.vel = (btn.inv * a.vel).let { (x, y) ->
                btn * Vec(x, y, 0f)
            }
        }


    }
}