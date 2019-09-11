package eu.metatools.nw.encoding

import java.io.Serializable

/**
 * Encoding that replaces non-encodeable Kotlin objects with serializable versions.
 */
open class KotlinEncoding<N, P> : SerializationEncoding<N, P>() {
    /**
     * Stand in object for Unit.
     */
    private object UnitStandIn : Serializable {
        private fun readResolve(): Any? = UnitStandIn
    }

    override fun replace(element: Any?) =
        when (element) {
            null -> null
            Unit -> UnitStandIn
            else -> super.replace(element)
        }

    override fun resolve(element: Any?) =
        when (element) {
            null -> null
            UnitStandIn -> Unit
            else -> super.resolve(element)
        }
}