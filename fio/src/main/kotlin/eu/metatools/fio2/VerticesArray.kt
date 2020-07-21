package eu.metatools.fio2

import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.BufferUtils
import java.util.*

class VerticesArray(val attributes: VertexAttributes, val maxVertices: Int) : BindVertices {
    private val attachments = WeakHashMap<ShaderProgram, Bind>()

    override val buffer = BufferUtils.newUnsafeByteBuffer(attributes.vertexSize * maxVertices);

    override fun position(element: Int) {
        buffer.position(element * attributes.vertexSize)
    }

    override fun close() {
        BufferUtils.disposeUnsafeByteBuffer(buffer)
    }

    override fun on(shader: ShaderProgram): Bind = attachments.getOrPut(shader) {
        object : Bind {
            private val compiled by lazy {
                attributes.map { it to shader.locateAttribute(it.alias) }
            }

            override fun bind() {
                val posBefore = buffer.position()

                for ((attribute, location) in compiled) {
                    buffer.position(attribute.offset)

                    shader.enableVertexAttribute(location)
                    shader.setVertexAttribute(location, attribute.numComponents, attribute.type, attribute.normalized, attributes.vertexSize, buffer)
                }

                buffer.position(posBefore)
            }

            override fun unbind() {
                for ((_, location) in compiled)
                    shader.disableVertexAttribute(location)
            }
        }
    }
}