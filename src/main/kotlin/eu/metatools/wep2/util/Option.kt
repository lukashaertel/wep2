package eu.metatools.wep2.util

import java.io.Serializable

/**
 * Optional entry.
 */
sealed class Option<V> : Serializable

/**
 * No value for the option.
 */
class None<V> : Option<V>(), Serializable

/**
 * A present value for the option.
 */
data class Just<V>(val item: V) : Option<V>(), Serializable

/**
 * Gets the actual value of the option, if the receiver is [None], throws an [IllegalStateException].
 */
inline fun <V> Option<V>.orFail(block: () -> Nothing): V =
    if (this is Just) {
        this.item
    } else {
        block()
    }