package eu.metatools.up.dt

/**
 * Single indirection layer for a [value] of type [T].
 */
data class Box<out T>(val value: T){
    override fun toString() =
        value.toString()
}