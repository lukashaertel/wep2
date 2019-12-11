package eu.metatools.ex.ents

/**
 * This entity is rendered.
 */
interface Rendered {
    /**
     * Renders the entity.
     */
    fun render(time: Double)
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