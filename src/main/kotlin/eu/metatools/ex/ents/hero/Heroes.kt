package eu.metatools.ex.ents.hero

/**
 * Enum of known heroes.
 */
enum class Heroes : HeroKind {
    /**
     * Pazu archer.
     */
    Pazu {
        override val label = "Pazu"
        override val radius = 0.3f
        override val spriteSet = SpriteSets.Pazu

        override fun stats(level: Int) = Stats(
            health = 25f + level * 5f,
            ammo = 10 + level * 2,
            baseDamage = 2f + level,
            bowInit = maxOf(
                0.25f,
                1f - level / 5f
            ),
            bowHold = 0.5f + level / 5f,
            bowDegrade = 1f / (2f + level),
            bowMin = 0.2f,
            projectileSpeed = 10f + level / 2f,
            speed = 2f + level / 5f,
            hitXP = 5 + 2 * level,
            deathXP = 15 + 5 * level
        )
    }
}