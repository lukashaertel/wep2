package eu.metatools.ex.ents.hero

import com.badlogic.gdx.graphics.Color
import eu.metatools.ex.Resources
import eu.metatools.ex.uiZ
import eu.metatools.fio.InOut
import eu.metatools.fio.data.Mat
import eu.metatools.fio.drawable.tint
import eu.metatools.fio.immediate.submit
import eu.metatools.fio.resource.get

/**
 * Solid white.
 */
private val healthBarDrawable by lazy {
    Resources.solid.get()
}

/**
 * Inset of health bar to left of screen.
 */
private const val healthBarInsetX = 32f

/**
 * Inset of health bar to bottom of screen.
 */
private const val healthBarInsetY = 32f

/**
 * Width of health bar.
 */
private const val healthBarWidth = 200f

/**
 * Height of health bar.
 */
private const val healthBarHeight = 20f

/**
 * Draws the health bar for the [hero].
 */
fun InOut.submitHealth(hero: Hero, time: Double) {
    // Draw health bar as fully filled black bar overlaid with partial yellow bar.
    submit(
        healthBarDrawable.tint(Color.BLACK), time, Mat
            .translation(healthBarInsetX, healthBarInsetY, uiZ)
            .scale(healthBarWidth, healthBarHeight)
            .translate(0.5f, 0.5f)
    )

    submit(
        healthBarDrawable.tint(Color.RED), time, Mat
            .translation(healthBarInsetX, healthBarInsetY, uiZ)
            .scale(hero.health.toFloat() * healthBarWidth / hero.stats.health.toFloat(), healthBarHeight)
            .translate(0.5f, 0.5f)
    )
}