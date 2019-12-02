package eu.metatools.up.kryo

import com.esotericsoftware.kryo.Registration
import com.esotericsoftware.kryo.util.DefaultClassResolver
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.full.superclasses

/**
 * On [getRegistration], returns the registration for the class itself or for any of the base types.
 */
class SubtypeClassResolver : DefaultClassResolver() {
    /**
     * Memorize last result.
     */
    private val cached = hashMapOf<Class<*>, Registration?>()

    override fun getRegistration(type: Class<*>): Registration? = cached.getOrPut(type) {
        // Get direct result.
        super.getRegistration(type)
        // Otherwise, get superclass result.
            ?: type.kotlin.superclasses
                .asSequence()
                .mapNotNull { getRegistration(it.java) }
                .firstOrNull()
    }
}