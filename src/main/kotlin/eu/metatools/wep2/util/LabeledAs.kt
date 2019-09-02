package eu.metatools.wep2.util

/**
 * Adds a [toString] method to the function.
 */
inline infix fun (() -> Unit).labeledAs(crossinline toString: () -> String) =
    object : (() -> Unit) by this {
        override fun toString() = toString()
    }

/**
 * Adds a [toString] method to the function.
 */
inline infix fun <T> ((T) -> Unit).labeledAs(crossinline toString: () -> String) =
    object : ((T) -> Unit) by this {
        override fun toString() = toString()
    }