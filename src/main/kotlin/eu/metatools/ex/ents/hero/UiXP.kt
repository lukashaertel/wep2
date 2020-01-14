package eu.metatools.ex.ents.hero

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import eu.metatools.ex.Resources
import eu.metatools.ex.shadowText
import eu.metatools.ex.uiZ
import eu.metatools.f2d.InOut
import eu.metatools.f2d.data.Mat
import eu.metatools.f2d.drawable.tint
import eu.metatools.f2d.immediate.submit
import eu.metatools.f2d.resource.get
import eu.metatools.f2d.tools.Location
import eu.metatools.f2d.tools.ReferText

/**
 * Solid white.
 */
private val xpBarDrawable by lazy {
    Resources.solid.get()
}

/**
 * Height of the XP bar.
 */
private const val xpBarHeight = 10f
/**
 * Inset of the text above the XP bar.
 */
private const val xpBarInset = 2f
/**
 * Size of the level font.
 */
private const val xpBarLevelSize = 32f
/**
 * Size of the level start and end XP font.
 */
private const val xpBarRangeSize = 24f

/**
 * Size of the current XP count font.
 */
private const val xpBarValueSize = 16f

/**
 * Level start XP font.
 */
private val xpRangeStartDrawable by lazy {
    Resources.segoe[ReferText(
        horizontal = Location.Start,
        vertical = Location.End
    )]
}

/**
 * Level font.
 */
private val xpLevelValueDrawable by lazy {
    Resources.segoe[ReferText(
        horizontal = Location.End,
        vertical = Location.End,
        bold = true
    )]
}

/**
 * Current XP font.
 */
private val xpValueDrawable by lazy {
    Resources.segoe[ReferText(
        horizontal = Location.Start,
        vertical = Location.End,
        bold = true
    )]
}

/**
 * Level end XP font.
 */
private val xpRangeEndDrawable by lazy {
    Resources.segoe[ReferText(
        horizontal = Location.End,
        vertical = Location.End
    )]
}

/**
 * Draws the XP interface for the [hero].
 */
fun InOut.submitXP(hero: Hero, time: Double) {
    // Get current range and fraction of fulfillment.
    val xpRange = XP.rangeFor(hero.level)
    val xpFraction = (hero.xp - xpRange.first).toFloat() / (xpRange.last + 1 - xpRange.first)

    // Compute all strings.
    val xpLevelString = hero.level.toString()
    val xpRangeStartString = xpRange.first.toString()
    val xpRangeEndString = xpRange.last.inc().toString()
    val xpValueString = "(${hero.xp})"


    // Draw XP bar as fully filled black bar overlaid with partial yellow bar.
    submit(
        xpBarDrawable.tint(Color.BLACK), time, Mat
            .translation(0f, 0f, uiZ)
            .scale(Gdx.graphics.width.toFloat(), xpBarHeight)
            .translate(0.5f, 0.5f)
    )

    submit(
        xpBarDrawable.tint(Color.YELLOW), time, Mat
            .translation(0f, 0f, uiZ)
            .scale(xpFraction * Gdx.graphics.width, xpBarHeight)
            .translate(0.5f, 0.5f)
    )

    // Draw XP values.
    shadowText(
        xpRangeStartDrawable, xpRangeStartString, time,
        xpBarInset, xpBarHeight + xpBarInset,
        xpBarRangeSize
    )
    shadowText(
        xpLevelValueDrawable, xpLevelString, time,
        Gdx.graphics.width / 2f - xpBarInset,
        xpBarHeight + xpBarInset,
        xpBarLevelSize
    )
    shadowText(
        xpValueDrawable, xpValueString, time,
        Gdx.graphics.width / 2f + xpBarInset, xpBarHeight + xpBarInset,
        xpBarValueSize
    )
    shadowText(
        xpRangeEndDrawable, xpRangeEndString, time,
        Gdx.graphics.width.toFloat() - xpBarInset,
        xpBarInset,
        xpBarRangeSize
    )
}