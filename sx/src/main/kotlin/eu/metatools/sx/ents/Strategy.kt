package eu.metatools.sx.ents

/**
 * A strategy that can be evaluated for cost and create a plan.
 */
interface Strategy<in T> {
    /**
     * Gets the cost for [on].
     * @param on the target.
     * @return Returns the cost or null if not applicable.
     */
    fun cost(on: T): Double?

    /**
     * Plans the strategy for [on].
     * @param on the target.
     * @return Returns the list of steps to perform the strategy.
     */
    fun plan(on: T): List<Step<T>>
}