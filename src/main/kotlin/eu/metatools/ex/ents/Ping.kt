package eu.metatools.ex.ents

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import eu.metatools.ex.Resources
import eu.metatools.ex.uiZ
import eu.metatools.f2d.InOut
import eu.metatools.f2d.data.Mat
import eu.metatools.f2d.drawable.tint
import eu.metatools.f2d.tools.Location
import eu.metatools.f2d.tools.ReferText
import eu.metatools.up.net.NetworkClock

/**
 * The text drawable for displaying the ping.
 */
private val pingDrawable by lazy {
    Resources.segoe[ReferText(
        horizontal = Location.End,
        vertical = Location.Start
    )].tint(Color.LIGHT_GRAY)
}

/**
 * Draws the ping to the interface.
 */
fun InOut.submitPing(clock: NetworkClock, time: Double) {
    // Render network offset (ping).
    submit(
        pingDrawable, "Offset: ${clock.currentDeltaTime}ms", time, Mat.translation(
            Gdx.graphics.width.toFloat() - 16f,
            Gdx.graphics.height.toFloat() - 16f,
            uiZ
        ).scale(24f, 24f)
    )
}