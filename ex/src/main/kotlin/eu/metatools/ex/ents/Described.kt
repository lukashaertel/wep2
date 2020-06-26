package eu.metatools.ex.ents

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import eu.metatools.ex.Resources
import eu.metatools.ex.shadowText
import eu.metatools.fio.InOut
import eu.metatools.fio.drawable.tint
import eu.metatools.fio.tools.Location
import eu.metatools.fio.tools.ReferText
import eu.metatools.up.isConnected

/**
 * Entity has a description.
 */
interface Described : All {
    /**
     * The description for an entity.
     */
    fun describe(): String
}


/**
 * Drawable for description.
 */
private val descriptionDrawable by lazy {
    Resources.segoe[ReferText(
        horizontal = Location.Center,
        vertical = Location.Start,
        bold = true
    )].tint(Color.YELLOW)
}

fun InOut.submitDescribed(described: Described?, time: Double) {
    // No describable given.
    if (described == null)
        return

    // Value is not connected.
    if (!described.isConnected())
        return

    // Submit the description.
    shadowText(
        descriptionDrawable, described.describe(), time,
        Gdx.graphics.width.toFloat() / 2f, Gdx.graphics.height - 16f, 32f
    )

}