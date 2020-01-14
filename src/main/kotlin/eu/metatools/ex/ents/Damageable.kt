package eu.metatools.ex.ents

import eu.metatools.f2d.data.Q

/**
 * Entity can take damage.
 */
interface Damageable : All {
    /**
     * Takes that damage.
     * @param amount The amount of damage to take.
     */
    fun takeDamage(amount: Q): Int
}