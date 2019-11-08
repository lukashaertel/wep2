package eu.metatools.up.aspects

/**
 * Converts between values and proxies.
 */
interface Proxify : Aspect {
    /**
     * Converts the given [value] to a proxy.
     */
    fun toProxy(value: Any?): Any?

    /**
     * Converts the given [proxy] to a value.
     */
    fun toValue(proxy: Any?): Any?
}

/**
 * If a set of aspects provide resolution of proxies, applies [Proxify.toProxy].
 */
fun Aspects?.toProxy(value: Any?) =
    if (value == null) null else with<Proxify>()?.toProxy(value) ?: value

/**
 * If a set of aspects provide resolution of proxies, applies [Proxify.toValue].
 */
fun Aspects?.toValue(value: Any?) =
    if (value == null) null else with<Proxify>()?.toValue(value) ?: value

