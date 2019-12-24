package eu.metatools.ex

fun <K, V> MutableMap<K, MutableList<V>>.add(key: K, value: V): Boolean {
    return getOrPut(key, ::mutableListOf).add(value)
}

fun <K, V> MutableMap<K, MutableList<V>>.remove(key: K, value: V): Boolean {
    val target = get(key)
        ?: return false

    val result = target.remove(value)
    if (result && target.isEmpty())
        remove(key)
    return result
}