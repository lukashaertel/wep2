package eu.metatools.wep2.nes.lang

/**
 * Checks that the [value] is true. Then returns `null`. Refers ot [check].
 */
fun validate(value: Boolean): Nothing? {
    check(value)
    return null
}

/**
 * Checks that the [value] is true. Then returns `null`. Passes the [lazyMessage] to the underlying [check].
 */
inline fun validate(value: Boolean, lazyMessage: () -> Any): Nothing? {
    check(value, lazyMessage)
    return null
}

/**
 * A value that never evaluates, i.e., throws an error indicating the fault of reaching this value.
 */
val never: Nothing
    get() {
        error("This value should never evaluate.")
    }