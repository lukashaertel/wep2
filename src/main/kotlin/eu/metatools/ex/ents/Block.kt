package eu.metatools.ex.ents

import eu.metatools.f2d.data.Q
import eu.metatools.f2d.data.QVec
import eu.metatools.f2d.data.Tri
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

    fun top(x: Q, y: Q): Q = Q.ONE

    fun bottom(x: Q, y: Q): Q = Q.ZERO


}

/**
 * Gets the absolute height at the position.
 */
fun Map<Tri, Block>.bindHeight(pos: QVec): Q? {
    val ax = (pos.x + Q.HALF).floor()
    val ay = (pos.y + Q.HALF).floor()
    val az = pos.z.floor()

    val dx = pos.x - ax.toQ()
    val dy = pos.y - ay.toQ()
    val dz = pos.z - az.toQ()

    val block = get(Tri(ax, ay, az)) ?: return null
    val top = block.top(dx, dy)
    val bottom = block.bottom(dx, dy)
    val relative = top.takeUnless { dz < bottom } ?: return null
    return relative.plus(az.toQ())
}

