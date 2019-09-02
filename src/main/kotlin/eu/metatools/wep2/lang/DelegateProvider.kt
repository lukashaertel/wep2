package eu.metatools.wep2.lang

import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Interface for delegate providers on read only properties.
 */
interface ReadOnlyPropertyProvider<R, T> {
    /**
     * Provides a delegate.
     * @param thisRef The receiver.
     * @param property The property the delegate is provided for.
     */
    operator fun provideDelegate(thisRef: R, property: KProperty<*>): ReadOnlyProperty<R, T>
}

/**
 * Interface for delegate providers on read/write properties.
 */
interface ReadWritePropertyProvider<R, T> {
    /**
     * Provides a delegate.
     * @param thisRef The receiver.
     * @param property The property the delegate is provided for.
     */
    operator fun provideDelegate(thisRef: R, property: KProperty<*>): ReadWriteProperty<R, T>
}

/**
 * Creates a read only property provider from the given lambda [block].
 */
fun <R, T> ReadOnlyPropertyProvider(block: (R, KProperty<*>) -> ReadOnlyProperty<R, T>) =
    object : ReadOnlyPropertyProvider<R, T> {
        override fun provideDelegate(thisRef: R, property: KProperty<*>) = block(thisRef, property)
    }

/**
 * Creates a read/write property provider from the given lambda [block].
 */
fun <R, T> ReadWritePropertyProvider(block: (R, KProperty<*>) -> ReadWriteProperty<R, T>) =
    object : ReadWritePropertyProvider<R, T> {
        override fun provideDelegate(thisRef: R, property: KProperty<*>) = block(thisRef, property)
    }

