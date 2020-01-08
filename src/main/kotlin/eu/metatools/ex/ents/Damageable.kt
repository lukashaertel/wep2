package eu.metatools.ex.ents

/**
 * Entity can take damage.
 */
interface Damageable : All {
    /**
     * Takes that damage.
     * @param amount The amount of damage to take.
     */
    fun takeDamage(amount: Int)
}