package eu.metatools.mk.util

/**
 * Choice entry.
 */
sealed class Choice<in L, in R>

/**
 * A present value for the left type of [Choice].
 */
data class Left<T>(val item: T) : Choice<T, Any?>()

/**
 * A present value for the right type of [Choice].
 */
data class Right<T>(val item: T) : Choice<Any?, T>()