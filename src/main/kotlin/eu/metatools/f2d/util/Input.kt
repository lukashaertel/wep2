package eu.metatools.f2d.util

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector2

/**
 * Touch x-coordinate in the projection space.
 */
val Input.uniformX: Float
    get() =
        x.toFloat() * 2f / Gdx.graphics.width - 1f

/**
 * Touch y-coordinates in the projection space.
 */
val Input.uniformY: Float
    get() =
        1f - y.toFloat() * 2f / Gdx.graphics.height

/**
 * True if the key is pressed.
 */
operator fun Input.contains(key: Int) =
    isKeyPressed(key)