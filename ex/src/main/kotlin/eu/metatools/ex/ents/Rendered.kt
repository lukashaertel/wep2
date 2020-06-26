package eu.metatools.ex.ents

import eu.metatools.fio.data.Mat

/**
 * This entity is rendered.
 */
interface Rendered {
    /**
     * Renders the entity.
     */
    fun render(mat: Mat, time: Double)
}

