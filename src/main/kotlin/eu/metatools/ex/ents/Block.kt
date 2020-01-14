package eu.metatools.ex.ents

import eu.metatools.f2d.data.Q
import eu.metatools.f2d.data.Tri
import eu.metatools.f2d.data.plus
import eu.metatools.f2d.data.toQ
import eu.metatools.f2d.drawable.Drawable

/**
 * World geometry and data block.
 */
interface Block {
    /**
     * The body (filling drawable).
     */
    val body: Drawable<Unit?>? get() = null

    /**
     * The cap (drawable for floor above).
     */
    val cap: Drawable<Unit?>? get() = null

    /**
     * True if block contributes to the hull.
     */
    val solid: Boolean get() = true

    /**
     * True if block contributes to walkable hull on level above.
     */
    val walkable: Boolean get() = true

    /**
     * Extra data.
     */
    val extras: Map<out Any, Any> get() = emptyMap()

    /**
     * True if being in this block evaluates hulls on level and level above (e.g., ramps, stairs).
     */
    val intermediate: Boolean get() = false

    /**
     * True if being in this block raises or lowers the level (e.g., ramps, stairs).
     */
    val lift: Int get() = 0

    /**
     * Evaluates the relative height when moving in this block (e.g., ramps, stairs).
     */
    fun height(x: Number, y: Number): Number = 0
}

/**
 * True if the block at the position is intermediate.
 */
fun Map<Tri, Block>.intermediate(level: Int, x: Number, y: Number): Boolean {
    val qx = x.toQ()
    val qy = y.toQ()

    val ax = (qx + Q.HALF).floor()
    val ay = (qy + Q.HALF).floor()

    val at = Tri(ax, ay, level)
    return get(at)?.intermediate ?: false
}

/**
 * Level raising or lowering per position.
 */
fun Map<Tri, Block>.lift(level: Int, x: Number, y: Number): Int {
    val qx = x.toQ()
    val qy = y.toQ()

    val ax = (qx + Q.HALF).floor()
    val ay = (qy + Q.HALF).floor()

    val at = Tri(ax, ay, level)
    return get(at)?.lift ?: 0
}

/**
 * Gets the absolute height at the position.
 */
fun Map<Tri, Block>.height(level: Int, x: Number, y: Number): Number {
    val qx = x.toQ()
    val qy = y.toQ()

    val ax = (qx + Q.HALF).floor()
    val ay = (qy + Q.HALF).floor()

    val dx = qx - ax.toQ()
    val dy = qy - ay.toQ()

    val at = Tri(ax, ay, level)
    return get(at)?.height(dx, dy)?.plus(level.toQ()) ?: level.toQ()
}

