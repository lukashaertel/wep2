package eu.metatools.ex.ents

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