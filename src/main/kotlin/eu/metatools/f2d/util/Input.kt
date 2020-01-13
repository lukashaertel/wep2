package eu.metatools.f2d.util

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input

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
 * Touch x-coordinate from the screen center.
 */
val Input.centeredX: Float
    get() =
        x.toFloat() - Gdx.graphics.width / 2f

/**
 * Touch y-coordinate from the screen center.
 */
val Input.centeredY: Float
    get() =
        Gdx.graphics.height / 2f - y.toFloat()


/**
 * True if the key is pressed.
 */
operator fun Input.contains(key: Int) =
    isKeyPressed(key)