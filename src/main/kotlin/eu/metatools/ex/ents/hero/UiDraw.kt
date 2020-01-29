package eu.metatools.ex.ents.hero

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import eu.metatools.ex.Resources
import eu.metatools.ex.uiZ
import eu.metatools.f2d.InOut
import eu.metatools.f2d.data.Mat
import eu.metatools.f2d.drawable.tint
import eu.metatools.f2d.immediate.submit
import eu.metatools.f2d.resource.get

/**
 * Solid white.
 */
private val drawBarDrawable by lazy {
    Resources.solid.get()
}

/**
 * Inset from top and bottom.
 */
private const val drawBarInsetV = 100f

/**
 * Width of the draw bar.
 */
private const val drawBarWidth = 48f

/**
 * Draws the draw bar for the [hero].
 */
fun InOut.submitDraw(hero: Hero, time: Double) {
    // The draw factor for when hero is drawing.
    val drawFactor = hero.bowDrawFactor(time)

    // The damage factor for when drawn.
    val damageFactor = hero.bowDamageFactor(time)

    // Check if still drawing.
    if (drawFactor != null && drawFactor < 1f) {
        // Display closing white or red (no ammo) bar.
        val color = if (hero.ammo == 0) Color.RED else Color.WHITE

        // Compute size of closing in bars.
        val size = (Gdx.graphics.height - 2f * drawBarInsetV) / 2.0f * drawFactor.toFloat()

        submit(
            drawBarDrawable.tint(color), time, Mat
                .translation(0f, drawBarInsetV, uiZ)
                .scale(drawBarWidth, size)
                .translate(0.5f, 0.5f)
        )

        submit(
            drawBarDrawable.tint(color), time, Mat
                .translation(Gdx.graphics.width - drawBarWidth, drawBarInsetV, uiZ)
                .scale(drawBarWidth, size)
                .translate(0.5f, 0.5f)
        )

        submit(
            drawBarDrawable.tint(color), time, Mat
                .translation(0f, Gdx.graphics.height - drawBarInsetV, uiZ)
                .scale(drawBarWidth, size)
                .translate(0.5f, -0.5f)
        )

        submit(
            drawBarDrawable.tint(color), time, Mat
                .translation(Gdx.graphics.width - drawBarWidth, Gdx.graphics.height - drawBarInsetV, uiZ)
                .scale(drawBarWidth, size)
                .translate(0.5f, -0.5f)
        )
    } else if (damageFactor != null) {
        // Drawn, draw interpolated between red to green or red if no ammo.
        val color = if (hero.ammo == 0) Color.RED else Color.RED.cpy().lerp(Color.GREEN, damageFactor.toFloat())

        submit(
            drawBarDrawable.tint(color), time, Mat
                .translation(0f, drawBarInsetV, uiZ)
                .scale(drawBarWidth, Gdx.graphics.height - 2 * drawBarInsetV)
                .translate(0.5f, 0.5f)
        )

        submit(
            drawBarDrawable.tint(color), time, Mat
                .translation(Gdx.graphics.width - drawBarWidth, drawBarInsetV, uiZ)
                .scale(drawBarWidth, Gdx.graphics.height - 2 * drawBarInsetV)
                .translate(0.5f, 0.5f)
        )
    }
}