package eu.metatools.ex.ents.hero

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.MathUtils
import eu.metatools.ex.Resources
import eu.metatools.ex.ents.Projectile
import eu.metatools.ex.shadowText
import eu.metatools.ex.uiZ
import eu.metatools.f2d.InOut
import eu.metatools.f2d.data.Mat
import eu.metatools.f2d.immediate.submit
import eu.metatools.f2d.tools.Location
import eu.metatools.f2d.tools.ReferText

/**
 * Ammo count text.
 */
private val ammoCountDrawable by lazy {
    Resources.segoe[ReferText(
        horizontal = Location.End,
        vertical = Location.Center
    )]
}

/**
 * Horiztontal offset of ammo display from left side.
 */
private const val ammoInsetX = 100f

/**
 * Vertical offset of ammo display from top.
 */
private const val ammoInsetY = 48f

/**
 * Length on which to display the ammo counter.
 */
private const val ammoLength = 600f

/**
 * Size of an arrow in the ammo counter.
 */
private const val ammoDisplaySize = 64f

/**
 * Spacing between ammo bar and ammo count text.
 */
private const val ammoCountSpacing = 16f

/**
 * Ammo count text size.
 */
private const val ammoCountSize = 32f

/**
 * Draws the ammo for the given [hero].
 */
fun InOut.submitAmmo(hero: Hero, time: Double) {
    // No ammo, return.
    if (hero.stats.ammo <= 0)
        return

    // Get step size.
    val dx = ammoLength / (hero.stats.ammo - 1)

    // Get y-coordinate for the row.
    val y = Gdx.graphics.height - ammoInsetY

    // Submit an arrow for number of ammo, spaced by given step size.
    for (i in 0 until hero.ammo)
        submit(
            Projectile.arrow, time, Mat
                .translation(ammoInsetX + dx * i, y, uiZ)
                .rotateZ(MathUtils.PI / 2f)
                .scale(ammoDisplaySize, ammoDisplaySize)
        )

    // Display a text for the amount of ammo.
    shadowText(
        ammoCountDrawable, "${hero.ammo}/${hero.stats.ammo}", time, ammoInsetX - ammoCountSpacing, y,
        ammoCountSize
    )
}