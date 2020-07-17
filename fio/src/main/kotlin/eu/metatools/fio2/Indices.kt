package eu.metatools.fio2

import com.badlogic.gdx.graphics.glutils.ShaderProgram
import java.nio.ByteBuffer

interface Bind {
    fun bind() = Unit
    fun unbind() = Unit
}

interface BindData : Bind, AutoCloseable {
    val buffer: ByteBuffer

    fun commit() = Unit
}

interface BindVertices : BindData {
    fun on(shader: ShaderProgram): Bind
}