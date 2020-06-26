package eu.metatools.fio.tools

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import eu.metatools.fio.context.Context
import eu.metatools.fio.resource.LifecycleDrawable
import eu.metatools.fio.resource.NotifyingResource
import java.util.*
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * Refers to a drawable in a [DataResource].
 * @property data The data to display.
 * @property transform The [Pixmap] generator.
 */
data class ReferData<T>(val data: T, val transform: (T) -> Pixmap) {
    fun createPixmap() = transform(data)
}

/**
 * Computes a square hash image from the byte array.
 */
fun hashImage(byteArray: ByteArray): Pixmap {
    if (byteArray.size < 4)
        return Pixmap(0, 0, Pixmap.Format.RGBA8888)

    val bitSet = BitSet.valueOf(byteArray)
    val a = bitSet[0]
    val b = bitSet[1]
    val c = bitSet[2]
    val d = bitSet[3]

    val fg = Color.rgba8888(if (a) 1f else 0.8f, if (a) 0.8f else 1f, if (b) 1f else 0.8f, 1.0f)
    val bg = Color.rgba8888(if (c) 0.2f else 0.4f, if (d) 0.2f else 0.4f, if (d) 0.4f else 0.2f, 1.0f)

    val length = bitSet.length() - 4
    val x = ceil(sqrt(length.toDouble())).toInt()
    val y = length / x

    val result = Pixmap(x, y, Pixmap.Format.RGBA8888)
    for (i in 0 until length)
        result.drawPixel(i % x, i / x, if (bitSet[4 + i]) fg else bg)

    return result
}

/**
 * Generates a texture from a data argument, with it providing the generation of a pixmap.
 */
class DataResource : NotifyingResource<ReferData<*>, LifecycleDrawable<Unit?>>() {
    override fun referNew(argsResource: ReferData<*>): LifecycleDrawable<Unit?> =
        object : LifecycleDrawable<Unit?> {

            private var texture: Texture? = null

            override fun initialize() {
                if (texture == null) {
                    val pixmap = argsResource.createPixmap()
                    texture = Texture(pixmap)
                    pixmap.dispose()
                }
            }

            override fun dispose() {
                texture?.dispose()
                texture = null
            }

            override fun draw(args: Unit?, time: Double, context: Context) {
                // Get texture or return if not assigned yet.
                val texture = texture ?: return

                // Draw to sprite batch.
                context.sprites().draw(texture, -0.5f, -0.5f, 1.0f, 1.0f)
            }
        }
}