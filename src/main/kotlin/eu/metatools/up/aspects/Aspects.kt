package eu.metatools.wep2.nes.aspects

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
 * Removes the given [KClass]es from the set of implemented [Aspect]s.
 * @property on The actual aspects.
 * @property removed The set of removed [KClass]es.
 */
data class Redact(val on: Aspects, val removed: Set<KClass<*>>) : Aspects {
    override fun <T : Aspect> with(type: KClass<T>) =
        // If the requested aspect is in the removed set, return null.
        if (type in removed) null else on.with(type)
}

/**
 * Redacts the given class [T] from the set of implemented [Aspect]s.
 */
inline fun <reified T : Aspect> Aspects.redact() =
    // If already in redaction, add the removed element to the set, otherwise create a new set.
    if (this is Redact)
        Redact(this.on, this.removed + T::class)
    else
        Redact(this, setOf(T::class))