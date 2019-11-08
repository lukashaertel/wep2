package eu.metatools.wep2.nes.lang

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * On setting the value, the previous value will be closed if present.
 */
fun autoClosing() = object : ReadWriteProperty<Any?, AutoCloseable?> {
    private var current: AutoCloseable? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>) =
        current

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: AutoCloseable?) {
        current?.close()
        current = value
    }
}