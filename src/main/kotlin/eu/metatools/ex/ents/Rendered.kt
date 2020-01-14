package eu.metatools.ex.ents

import eu.metatools.f2d.data.Mat

/**
 * This entity is rendered.
 */
interface Rendered {
    /**
     * Renders the entity.
     */
    fun render(mat: Mat, time: Double)
}

