package eu.metatools.up.dt

import java.io.*
import kotlin.reflect.KClass

/**
 * A serializable proxy on [KClass]es.
 */
data class KClassProxy<T : Any>(val kClass: KClass<T>) : Externalizable {
    @Suppress("unchecked_cast")
    private constructor() : this(Nothing::class as KClass<T>)

    override fun readExternal(input: ObjectInput) {
        @Suppress("val_reassignment", "unchecked_cast")
        kClass = Class.forName(input.readUTF()).kotlin as KClass<T>
    }

    override fun writeExternal(output: ObjectOutput) {
        output.writeUTF(kClass.java.name)
    }
}