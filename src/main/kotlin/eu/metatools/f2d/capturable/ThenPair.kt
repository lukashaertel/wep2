package eu.metatools.f2d.capturable

/**
 * Concatenates two [Capturable]s with different argument types, resulting in a pair.
 */
infix fun <T, U> Capturable<T>.thenPair(next: Capturable<U>) = then<Pair<T, U>, T, U>(next, { it.first }, { it.second })

/**
 * Concatenates two [Capturable]s with different argument types, resulting in a pair.
 */
@JvmName("thenPairNullable")
infix fun <T : Any, U : Any> Capturable<T?>.thenPair(next: Capturable<U?>) =
    then<Pair<T, U>?, T?, U?>(next, { it?.first }, { it?.second })