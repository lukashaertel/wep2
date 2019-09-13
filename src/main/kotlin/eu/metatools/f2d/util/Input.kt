package eu.metatools.f2d.util

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector2

/**
 * Mouse coordinates in the projection space.
 */
val Input.uniformMouseCoords: Vector2
    get() =
        Vector2(
            x.toFloat() * 2f / Gdx.graphics.width - 1f,
            1f - y.toFloat() * 2f / Gdx.graphics.height
        )