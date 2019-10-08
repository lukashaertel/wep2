package eu.metatools.wep2.storage

import eu.metatools.wep2.util.Just
import eu.metatools.wep2.util.delegates.MutableProperty
import eu.metatools.wep2.util.None
import eu.metatools.wep2.util.Option
import eu.metatools.wep2.util.labeledAs
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * From a [restore] object, loads the item at the [key]. If [proxy] is given as true, the read object will be passed
 * through [resolve] on the post-restore hook. Otherwise, the item will be used as is. If not restoring, i.e., if the
 * [restore] object is `null`, the [generate] function will provide the initial value. Once initialization happened, the
 * [initialized] callback is run. The result is a delegate at some point returning the desired actual value.
 */
inline fun <P, V> loadProxified(
    restore: Restore?,
    key: String,
    proxy: Boolean,
    generate: () -> V?,
    crossinline resolve: (P) -> V?,
    crossinline initialized: (V?) -> Unit
): ReadWriteProperty<Any?, V?> {
    // Not restoring, directly return value.
    if (restore == null)
        return MutableProperty(generate()).also {
            // Notify listener.
            initialized(it.current)
        }

    // Restoring but no proxy, load from restore object.
    if (!proxy)
        return MutableProperty(restore.load<V?>(key)).also {
            // Notify listener.
            initialized(it.current)
        }

    // Restoring with proxy.
    return object : ReadWriteProperty<Any?, V?> {
        /**
         * The proxy object to load from.
         */
        private val from = restore.load<P?>(key)

        /**
         * The current value, initialized on [None].
         */
        private var current: Option<V?> = None

        init {
            // Register a post operation to resolve the proxy.
            restore.registerPost({
                // Resolve via application of resolution.
                val resolved = if (from == null) null else resolve(from)

                // Assign to value, notify listener.
                current = Just(resolved)
                initialized(resolved)
            } labeledAs { "Resolving $from" })
        }

        override fun getValue(thisRef: Any?, property: KProperty<*>) =
            (current as Just).item

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: V?) {
            current = Just(value)
        }
    }
}

/**
 * Stores into a [store] a [value] under [key]. If [proxy] is true, [proxify] will transform the value beforehand.
 */
inline fun <P, V> storeProxified(store: Store, key: String, proxy: Boolean, value: V, proxify: (V) -> P) {
    if (proxy)
        store.save(key, proxify(value))
    else
        store.save(key, value)
}