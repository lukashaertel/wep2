package eu.metatools.wep2.aspects

/**
 * Marks an object that supports resolving.
 * @param I The type of the index keys.
 * @param S The subjects that are resolved.
 */
interface Resolving<in I : Comparable<I>, S> {
    /**
     * Resolves the value at the [index].
     */
    fun resolve(index: I): S?
}