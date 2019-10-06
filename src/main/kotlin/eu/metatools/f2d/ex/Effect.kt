package eu.metatools.f2d.ex

import java.io.Serializable

/**
 * A transformation function.
 */
typealias Effect<T> = (T) -> T

/**
 * A sequence of functions.
 */
data class Effects<T>(val parts: List<Effect<T>>) : Effect<T>, Serializable {
    override fun invoke(p1: T) =
        parts.fold(p1) { r, f -> f(r) }
}