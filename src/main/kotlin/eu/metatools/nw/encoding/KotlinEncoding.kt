package eu.metatools.nw.encoding

import java.io.Serializable

open class KotlinEncoding<N, P> : SerializationEncoding<N, P>() {
    private object UnitStandIn : Serializable

    override fun replace(element: Any?) =
        when (element) {
            Unit -> UnitStandIn
            else -> super.replace(element)
        }

    override fun resolve(element: Any?) =
        when (element) {
            UnitStandIn -> Unit
            else -> super.resolve(element)
        }
}