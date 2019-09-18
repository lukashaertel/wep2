package eu.metatools.f2d.math

import com.badlogic.gdx.math.Matrix4
import kotlin.math.*


@Suppress("nothing_to_inline")
internal inline fun roundForPrint(value: Float) =
    round(value * 1e5) / 1e5

/**
 * Preliminary coordinate delegate, uses a four dimensional matrix.
 */
typealias Coords = Matrix4

/**
 * Retrieves the coordinates at a given time.
 */
typealias CoordsAt = (Double) -> Coords