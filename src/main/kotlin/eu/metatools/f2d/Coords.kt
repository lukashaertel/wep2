package eu.metatools.f2d

import com.badlogic.gdx.math.Matrix4

/**
 * Preliminary coordinate delegate, uses a four dimensional matrix.
 */
typealias Coords = Matrix4

/**
 * Retrieves the coordinates at a given time.
 */
typealias CoordsAt = (Double) -> Coords