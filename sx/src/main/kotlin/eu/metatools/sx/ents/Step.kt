package eu.metatools.sx.ents

/**
 * A step in a strategy's plan.
 */
interface Step<in T> {
    /**
     * Acts the step on the target.
     * @param on The target.
     * @param available The available time.
     * @return Returns the result of doing the step with the given time.
     */
    fun act(on: T, available: Double): StepResult
}