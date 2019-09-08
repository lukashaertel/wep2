package eu.metatools.f2d.wep2.encoding

import com.badlogic.gdx.graphics.Color
import eu.metatools.nw.encoding.KotlinEncoding
import java.io.Serializable

class GdxEncoding<N, P> : KotlinEncoding<N, P>() {
    private data class ColorStandIn(val r: Float, val g: Float, val b: Float, val a: Float) : Serializable

    override fun replace(element: Any?) =
        when (element) {
            is Color -> ColorStandIn(element.r, element.g, element.b, element.a)
            else -> super.replace(element)
        }

    override fun resolve(element: Any?) =
        when (element) {
            is ColorStandIn -> Color(element.r, element.g, element.b, element.a)
            else -> super.resolve(element)
        }
}