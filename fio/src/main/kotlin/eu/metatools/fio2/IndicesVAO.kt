package eu.metatools.fio2

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.utils.BufferUtils

class IndexVAO(val static: Boolean, val short: Boolean, val maxIndices: Int) : BindData {
    override val buffer = BufferUtils.newUnsafeByteBuffer(maxIndices * if (short) 2 else 4)

    val usage = if (static) GL20.GL_STATIC_DRAW else GL20.GL_DYNAMIC_DRAW

    val handle = Gdx.gl20.glGenBuffer().also {
        Gdx.gl20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, it)
        Gdx.gl20.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER, buffer.capacity(), null, usage)
        Gdx.gl20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    override fun bind() {
        Gdx.gl20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, handle)
    }

    override fun position(element: Int) {
        if (short)
            buffer.position(element * 2)
        else
            buffer.position(element * 4)
    }

    override fun commit(start: Int, end: Int?) {
        val byteStart = start * if (short) 2 else 4
        val byteEnd = end?.times(if (short) 2 else 4) ?: buffer.limit()
        Gdx.gl20.glBufferSubData(GL20.GL_ELEMENT_ARRAY_BUFFER, byteStart, byteEnd - byteStart, buffer.position(byteStart))
    }

    override fun unbind() {
        Gdx.gl20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    override fun close() {
        Gdx.gl20.glDeleteBuffer(handle)
        BufferUtils.disposeUnsafeByteBuffer(buffer)
    }
}