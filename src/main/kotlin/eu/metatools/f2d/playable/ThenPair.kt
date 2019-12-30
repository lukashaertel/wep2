package eu.metatools.f2d.playable

/**
 * Concatenates two [Playable]s with different argument types, resulting in a pair.
 */
infix fun <T, U> Playable<T>.thenPair(next: Playable<U>) =
    then<Pair<T, U>, T, U>(next, { it.first }, { it.second })

/**
 * Concatenates two [Playable]s with different argument types, resulting in a pair.
 */
@JvmName("thenPairNullable")
infix fun <T : Any, U : Any> Playable<T?>.thenPair(next: Playable<U?>) =
    then<Pair<T, U>?, T?, U?>(next, { it?.first }, { it?.second })