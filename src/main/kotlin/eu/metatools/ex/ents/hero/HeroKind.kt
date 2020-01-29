package eu.metatools.ex.ents.hero

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
    val radius: Float

    /**
     * Sprite set to use.
     */
    val spriteSet: SpriteSet

    /**
     * Stats at level.
     */
    fun stats(level: Int): Stats
}