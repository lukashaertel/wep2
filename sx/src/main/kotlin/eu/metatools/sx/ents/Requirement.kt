package eu.metatools.sx.ents

/**
 * A requirement that can be addressed by strategies.
 */
interface Requirement<in T> {
    /**
     * The pressure of this requirement for [on].
     * @param on the target.
     * @return Returns the pressure or null if not applicable.
     */
    fun pressure(on: T): Double?

    /**
     * Enumerates the strategies for [on] to address the requirement.
     * @param on the target.
     * @return Returns a list of strategies.
     */
    fun strategies(on: T): List<Strategy<T>>
}