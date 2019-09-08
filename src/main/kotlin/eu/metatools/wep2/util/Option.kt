package eu.metatools.wep2.util

import java.io.Serializable

/**
 * Optional entry.
 */
sealed class Option<in V> : Serializable

/**
 * No value for the option.
 */
object None : Option<Any?>(), Serializable

/**
 * A present value for the option.
 */
data class Just<V>(val item: V) : Option<V>(), Serializable
