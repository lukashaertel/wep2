package eu.metatools.wep2.nes.dt

import java.io.Serializable

/**
 * Single indirection layer for a [value] of type [T].
 */
data class Box<T>(val value: T) : Serializable