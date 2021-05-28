package eu.metatools.sx.ents

/**
 * A running or completed plan.
 * @property requirement The requirement that was addressed.
 * @property strategy The applied strategy.
 * @property cost The associated cost of the plan when planned.
 * @property done The steps that were completed.
 * @property upcoming The steps that are/were to do.
 */
data class Plan<in T>(
    val requirement: Requirement<T>,
    val strategy: Strategy<T>,
    val cost: Double,
    val done: List<Step<T>>,
    val upcoming: List<Step<T>>
) {
    /**
     * True if the plan has [done] steps.
     */
    fun isStarted() = done.isNotEmpty()

    /**
     * True if the plan has no [upcoming] steps.
     */
    fun isDone() = upcoming.isEmpty()

    /**
     * True if only one step left in [upcoming]
     */
    fun isLast() = upcoming.size == 1

    /**
     * The currently working step.
     */
    fun current() = upcoming.first()

    /**
     * The plan after this, i.e., with the [current] step completed.
     */
    fun next() = Plan(requirement, strategy, cost, done + upcoming.take(1), upcoming.drop(1))
}