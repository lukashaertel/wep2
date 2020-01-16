package eu.metatools.ex.ents.hero

import eu.metatools.f2d.data.Q
import eu.metatools.f2d.data.over
import eu.metatools.f2d.data.plus
import eu.metatools.f2d.data.toQ

/**
 * Enum of known heroes.
 */
enum class Heroes : HeroKind {
    /**
     * Pazu archer.
     */
    Pazu {
        override val label = "Pazu"
        override val radius = Q.THIRD
        override val spriteSet = SpriteSets.Pazu

        override fun stats(level: Int) = Stats(
            health = 25.toQ() + level * 5,
            ammo = 10 + level * 2,
            baseDamage = 2 + level,
            bowInit = maxOf(
                Q.QUARTER,
                Q.ONE - level.over(5)
            ),
            bowHold = 1.over(2) + level.over(5),
            bowDegrade = 1.over(2 + level),
            bowMin = 1 over 5,
            projectileSpeed =  10f + level.over(2),
            speed = 2 + level.over(5),
            hitXP = 5 + 2 * level,
            deathXP = 15 + 5 * level
        )
    }
}