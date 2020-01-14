package eu.metatools.ex.ents.hero

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import eu.metatools.ex.solidDrawable
import eu.metatools.ex.uiZ
import eu.metatools.f2d.InOut
import eu.metatools.f2d.data.Mat
import eu.metatools.f2d.data.Q
import eu.metatools.f2d.drawable.tint
import eu.metatools.f2d.immediate.submit

/**
 * Inset from top and bottom.
 */
private const val drawInsetV = 100f

/**
 * Width of the draw bar.
 */
private const val drawWidth = 48f

/**
 * Draws the draw bar for the [hero].
 */
fun InOut.submitDraw(hero: Hero, time: Double) {
    // The draw factor for when hero is drawing.
    val drawFactor = hero.bowDrawFactor(time)

    // The damage factor for when drawn.
    val damageFactor = hero.bowDamageFactor(time)

    // Check if still drawing.
    if (drawFactor != null && drawFactor < Q.ONE) {
        // Display closing white or red (no ammo) bar.
        val color = if (hero.ammo == 0) Color.RED else Color.WHITE

        // Compute size of closing in bars.
        val size = (Gdx.graphics.height - 2f * drawInsetV) / 2.0f * drawFactor.toFloat()

        submit(
            solidDrawable.tint(color), time, Mat
                .translation(0f, drawInsetV, uiZ)
                .scale(drawWidth, size)
                .translate(0.5f, 0.5f)
        )

        submit(
            solidDrawable.tint(color), time, Mat
                .translation(Gdx.graphics.width - drawWidth, drawInsetV, uiZ)
                .scale(drawWidth, size)
                .translate(0.5f, 0.5f)
        )

        submit(
            solidDrawable.tint(color), time, Mat
                .translation(0f, Gdx.graphics.height - drawInsetV, uiZ)
                .scale(drawWidth, size)
                .translate(0.5f, -0.5f)
        )

        submit(
            solidDrawable.tint(color), time, Mat
                .translation(Gdx.graphics.width - drawWidth, Gdx.graphics.height - drawInsetV, uiZ)
                .scale(drawWidth, size)
                .translate(0.5f, -0.5f)
        )
    } else if (damageFactor != null) {
        // Drawn, draw interpolated between red to green or red if no ammo.
        val color = if (hero.ammo == 0) Color.RED else Color.RED.cpy().lerp(Color.GREEN, damageFactor.toFloat())

        submit(
            solidDrawable.tint(color), time, Mat
                .translation(0f, drawInsetV, uiZ)
                .scale(drawWidth, Gdx.graphics.height - 2 * drawInsetV)
                .translate(0.5f, 0.5f)
        )

        submit(
            solidDrawable.tint(color), time, Mat
                .translation(Gdx.graphics.width - drawWidth, drawInsetV, uiZ)
                .scale(drawWidth, Gdx.graphics.height - 2 * drawInsetV)
                .translate(0.5f, 0.5f)
        )
    }
}