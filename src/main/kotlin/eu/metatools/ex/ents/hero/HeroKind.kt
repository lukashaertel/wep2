package eu.metatools.ex.ents.hero

import eu.metatools.f2d.data.Q

/**
 * Kind of heroes.
 */
interface HeroKind {
    /**
     * Label for description.
     */
    val label: String

    /**
     * Radius.
     */
    val radius: Q

    /**
     * Sprite set to use.
     */
    val spriteSet: SpriteSet

    /**
     * Stats at level.
     */
    fun stats(level: Int): Stats
}