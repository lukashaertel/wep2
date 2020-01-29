package eu.metatools.ex.ents

import eu.metatools.ex.data.Mesh
import eu.metatools.ex.data.box
import eu.metatools.ex.math.sp
import eu.metatools.f2d.data.*
import eu.metatools.f2d.drawable.Drawable
import java.util.*

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
     * Extra data.
     */
    val extras: Map<out Any, Any> get() = emptyMap()

    /**
     * Generates the triangles for this block.
     */
    fun mesh(x: Float, y: Float, z: Float): Mesh = box(x, y, z)
}