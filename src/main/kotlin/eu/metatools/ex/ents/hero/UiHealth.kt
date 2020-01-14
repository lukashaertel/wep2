package eu.metatools.ex.ents.hero

import com.badlogic.gdx.graphics.Color
import eu.metatools.ex.solidDrawable
import eu.metatools.f2d.InOut
import eu.metatools.f2d.data.Mat
import eu.metatools.f2d.drawable.tint
import eu.metatools.f2d.immediate.submit

/**
 * Draws the health bar for the [hero].
 */
fun InOut.submitHealth(hero: Hero, time: Double) {
    // TODO: Extract constants and maybe redesign.

    // Draw XP bar as fully filled black bar overlaid with partial yellow bar.
    submit(
        solidDrawable.tint(Color.BLACK), time, Mat
            .translation(32f, 32f, -50f)
            .scale(200f, 20f)
            .translate(0.5f, 0.5f)
    )

    submit(
        solidDrawable.tint(Color.RED), time, Mat
            .translation(32f, 32f, -50f)
            .scale(hero.health.toFloat() * 200f / hero.stats.health.toFloat(), 20f)
            .translate(0.5f, 0.5f)
    )
}