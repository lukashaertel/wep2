package eu.metatools.up.aspects

/**
 * Provides extra constructor arguments.
 */
interface Construct : Aspect {
    val constructorArgs: Map<String, Any?>
}