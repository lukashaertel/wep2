package eu.metatools.ex.ents

import eu.metatools.f2d.data.*
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
     * Evaluates the relative height when moving in this block (e.g., ramps, stairs).
     */
    fun height(x: Number, y: Number): Number = Q.ONE
}

private const val maxDZ = 4
/**
 * Gets the absolute height at the position.
 */
fun Map<Tri, Block>.height(pos: QVec): Q? {
    val ax = (pos.x + Q.HALF).floor()
    val ay = (pos.y + Q.HALF).floor()
    val az = pos.z.floor()

    val dx = pos.x - ax.toQ()
    val dy = pos.y - ay.toQ()

    for (dz in 0..maxDZ) {
        val z = az - dz
        get(Tri(ax, ay, z))?.height(dx, dy)?.plus(z.toQ())?.let {
            return it
        }
    }

    return null
}

