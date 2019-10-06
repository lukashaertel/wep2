package eu.metatools.f2d.ex

/**
 * Uninitialized function.
 */
fun <T> undefined(): () -> T = { throw NoSuchElementException("Parameter is undefined") }