package eu.metatools.f2d.math

import kotlin.math.round


@Suppress("nothing_to_inline")
internal inline fun roundForPrint(value: Float) =
    round(value * 1e5) / 1e5


// TODO: Introduce color object.

typealias Transform = Mat

/**
 * Retrieves the coordinates at a given time.
 */
typealias CoordsAt = (Double) -> Transform