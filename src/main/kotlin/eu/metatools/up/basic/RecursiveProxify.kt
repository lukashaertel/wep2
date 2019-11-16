package eu.metatools.up.basic

import eu.metatools.up.aspects.Aspects
import eu.metatools.up.aspects.Proxify
import eu.metatools.up.aspects.With
import eu.metatools.up.dt.Lx
import eu.metatools.up.structure.Id

/**
 * Recursively applies proxification to some known data types. Uses [resolve] to get the actual value of an [Lx] as
 * an [Id] object.
 */
class RecursiveProxify(on: Aspects, val resolve: (Lx) -> Id) : With(on), Proxify {
    override fun toProxy(value: Any?): Any? =
        when (value) {
            // Resolve identified object to it's Lx.
            is Id -> value.id

            // Recursively apply to list elements.
            is List<*> -> value.mapTo(arrayListOf()) {
                toProxy(it)
            }

            // Recursively apply to array elements.
            is Array<*> -> Array(value.size) {
                toProxy(value[it])
            }

            // Recursively apply to set entries.
            is Set<*> -> value.mapTo(mutableSetOf()) {
                toProxy(it)
            }

            // Recursively apply to map entries.
            is Map<*, *> -> value.entries.associateTo(mutableMapOf()) {
                toProxy(it.key) to toProxy(it.value)
            }

            // Recursively apply to triple entries.
            is Triple<*, *, *> -> Triple(toProxy(value.first), toProxy(value.second), toProxy(value.third))

            // Recursively apply to pair entries.
            is Pair<*, *> -> Pair(toProxy(value.first), toProxy(value.second))

            // Return just the value.
            else -> value
        }

    override fun toValue(proxy: Any?): Any? =
        when (proxy) {
            // Resolve Lx to identified object.
            is Lx -> resolve(proxy)

            // Recursively apply to list elements.
            is List<*> -> proxy.mapTo(arrayListOf()) {
                toValue(it)
            }

            // Recursively apply to array elements.
            is Array<*> -> Array(proxy.size) {
                toValue(proxy[it])
            }


            // Recursively apply to set entries.
            is Set<*> -> proxy.mapTo(mutableSetOf()) {
                toValue(it)
            }

            // Recursively apply to map entries.
            is Map<*, *> -> proxy.entries.associateTo(mutableMapOf()) {
                toValue(it.key) to toValue(it.value)
            }

            // Recursively apply to triple entries.
            is Triple<*, *, *> -> Triple(toValue(proxy.first), toValue(proxy.second), toValue(proxy.third))

            // Recursively apply to pair entries.
            is Pair<*, *> -> Pair(toValue(proxy.first), toValue(proxy.second))

            // Return just the value.
            else -> proxy
        }
}