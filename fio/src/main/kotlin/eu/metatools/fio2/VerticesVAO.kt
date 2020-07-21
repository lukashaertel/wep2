package eu.metatools.fio2

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.BufferUtils
import java.util.*

class VerticesVAO(val static: Boolean, val attributes: VertexAttributes, val maxVertices: Int) : BindVertices {
    private val attachments = WeakHashMap<ShaderProgram, Bind>()

    private val usage = if (static) GL20.GL_STATIC_DRAW else GL20.GL_DYNAMIC_DRAW

    override val buffer = BufferUtils.newUnsafeByteBuffer(attributes.vertexSize * maxVertices);

    val handle = Gdx.gl20.glGenBuffer().also {
        Gdx.gl20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, it)
        Gdx.gl20.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER, buffer.capacity(), null, usage)
        Gdx.gl20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    val handleVAO = IntArray(1).let {
        Gdx.gl30.glGenVertexArrays(1, it, 0)
        it.single()
    }

    override fun bind() {
        Gdx.gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, handle)
    }

    override fun position(element: Int) {
        buffer.position(element * attributes.vertexSize)
    }

    override fun commit(start: Int, end: Int?) {
        val byteStart = start * attributes.vertexSize
        val byteEnd = end?.times(attributes.vertexSize) ?: buffer.limit()
        Gdx.gl.glBufferSubData(GL20.GL_ARRAY_BUFFER, byteStart, byteEnd - byteStart, buffer.position(byteStart))
    }

    override fun unbind() {
        Gdx.gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0)
    }

    override fun close() {
        Gdx.gl.glDeleteBuffer(handle)
        Gdx.gl30.glDeleteVertexArrays(1, intArrayOf(handleVAO), 0)
        BufferUtils.disposeUnsafeByteBuffer(buffer)
    }

    override fun on(shader: ShaderProgram): Bind = attachments.getOrPut(shader) {
        object : Bind {
            private val compiled by lazy {
                attributes.map { it to shader.locateAttribute(it.alias) }
            }

            override fun bind() {
                Gdx.gl30.glBindVertexArray(handleVAO)
                Gdx.gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, handle);

                for ((attribute, location) in compiled) {
                    shader.enableVertexAttribute(location)
                    shader.setVertexAttribute(location, attribute.numComponents, attribute.type, attribute.normalized, attributes.vertexSize, attribute.offset)
                }
            }

            override fun unbind() {
                for ((_, location) in compiled)
                    shader.disableVertexAttribute(location)

                Gdx.gl30.glBindVertexArray(0)
            }
        }
    }
}