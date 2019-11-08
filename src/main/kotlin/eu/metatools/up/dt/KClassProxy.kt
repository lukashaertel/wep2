package eu.metatools.wep2.nes.dt

import java.io.*
import kotlin.reflect.KClass

/**
 * A serializable proxy on [KClass]es.
 */
data class KClassProxy<T : Any>(val kClass: KClass<T>) : Serializable {
    @Suppress("unchecked_cast")
    private constructor() : this(Nothing::class as KClass<T>)

    private fun readObject(input: ObjectInputStream) {
        @Suppress("val_reassignment", "unchecked_cast")
        kClass = Class.forName(input.readUTF()).kotlin as KClass<T>
    }

    private fun writeObject(output: ObjectOutputStream) {
        output.writeUTF(kClass.java.name)
    }
}