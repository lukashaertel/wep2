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

/**
 * This interface receives periodic updates.
 */
interface Ticking {
    /**
     * Updates the state.
     * @param sec The current time.
     * @param freq The frequency between updates.
     */
    fun update(sec: Double, freq: Long)
}