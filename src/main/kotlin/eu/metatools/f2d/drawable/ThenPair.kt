package eu.metatools.f2d.drawable

/**
 * Concatenates two [Drawable]s with different argument types, resulting in a pair.
 */
infix fun <T, U> Drawable<T>.thenPair(next: Drawable<U>) = then<Pair<T, U>, T, U>(next, { it.first }, { it.second })

/**
 * Concatenates two [Drawable]s with different argument types, resulting in a pair.
 */
@JvmName("thenPairNullable")
infix fun <T : Any, U : Any> Drawable<T?>.thenPair(next: Drawable<U?>) =
    then<Pair<T, U>?, T?, U?>(next, { it?.first }, { it?.second })