package eu.metatools.wep2.util

import java.lang.IllegalStateException

/**
 * Optional entry.
 */
sealed class Option<in V>

/**
 * No value for the option.
 */
object None : Option<Any?>()

/**
 * A present value for the option.
 */
data class Just<V>(val item: V) : Option<V>()
