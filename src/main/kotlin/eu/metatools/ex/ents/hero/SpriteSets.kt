package eu.metatools.ex.ents.hero

import eu.metatools.ex.animation
import eu.metatools.ex.atlas
import eu.metatools.ex.data.Dir
import eu.metatools.f2d.drawable.Drawable

/**
 * Enum of known sprite sets.
 */
enum class SpriteSets : SpriteSet {
    Pazu {
        private val idleUp by atlas("pa_i_u")
        private val idleRight: Drawable<Unit?> by atlas("pa_i_r")
        private val idleDown: Drawable<Unit?> by atlas("pa_i_d")
        private val idleLeft: Drawable<Unit?> by atlas("pa_i_l")

        override fun idle(dir: Dir) =
            dir.select(idleUp, idleRight, idleDown, idleLeft)

        private val moveUp: Drawable<Unit?> by animation(0.8, "pa_w1_u", "pa_i_u", "pa_w2_u", "pa_i_u")
        private val moveRight: Drawable<Unit?> by animation(0.8, "pa_w1_r", "pa_i_r", "pa_w2_r", "pa_i_r")
        private val moveDown: Drawable<Unit?> by animation(0.8, "pa_w1_d", "pa_i_d", "pa_w2_d", "pa_i_d")
        private val moveLeft: Drawable<Unit?> by animation(0.8, "pa_w1_l", "pa_i_l", "pa_w2_l", "pa_i_l")

        override fun move(dir: Dir) =
            dir.select(moveUp, moveRight, moveDown, moveLeft)

        private val drawUp: Drawable<Unit?> by atlas("pa_d_u")
        private val drawRight: Drawable<Unit?> by atlas("pa_d_r")
        private val drawDown: Drawable<Unit?> by atlas("pa_d_d")
        private val drawLeft: Drawable<Unit?> by atlas("pa_d_l")

        override fun draw(dir: Dir) =
            dir.select(drawUp, drawRight, drawDown, drawLeft)
    }
}