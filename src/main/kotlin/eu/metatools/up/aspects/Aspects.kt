package eu.metatools.up.aspects

import kotlin.reflect.KClass
import kotlin.reflect.full.cast

/**
 * Marker for aspects.
 */
interface Aspect

/**
 * An aspect provider.
 */
interface Aspects {
    /**
     * Returns the implementation of [type] or `null`.
     */
    fun <T : Aspect> with(type: KClass<T>): T?
}

/**
 * If receiver is not `null`, returns the implementation of [T] or `null`.
 */
inline fun <reified T : Aspect> Aspects?.with() =
    this?.with(T::class)

/**
 * If receiver is not `null` and implements [T], runs the [block] with the value. Returns the implementation
 * or `null`.
 */
inline operator fun <reified T : Aspect> Aspects?.invoke(block: T.() -> Unit) =
    with<T>()?.apply(block)

/**
 * Implements aspects in the subtype, if an aspect is not implemented, it is delegated to [on] if given as not null.
 */
abstract class With(val on: Aspects? = null) : Aspects {
    override fun <T : Aspect> with(type: KClass<T>) =
        if (type.isInstance(this))
            type.cast(this)
        else
            on?.with(type)
}


/**
 * Aspect composition receiver.
 */
interface Compose {
    /**
     * Receives the [aspect].
     */
    fun receive(aspect: Aspect)
}

/**
 * Composes a set of aspects via the [Compose.receive] method. The set of constructed aspects is also available to
 * the scope as the single parameter to the [block].
 */
fun compose(block: Compose.(Aspects) -> Unit): Aspects {
    // Receive instances and cache results of with.
    val received = mutableListOf<Aspect>()
    val cached = mutableMapOf<KClass<*>, Aspect?>()

    // Receiver sends to list of instances and clears the cache.
    val receiver = object : Compose {
        override fun receive(aspect: Aspect) {
            received.add(aspect)
            cached.clear()
        }
    }

    // Aspects find instances of the type.
    val aspects = object : Aspects {
        override fun <T : Aspect> with(type: KClass<T>) =
            cached.getOrPut(type) {
                // Get first value that is an instance.
                received.firstOrNull(type::isInstance)
            }?.let {
                // If value is not null, cast it.
                type.cast(it)
            }
    }

    // Collect user created aspects, return the resulting set.
    block(receiver, aspects)
    return aspects
}