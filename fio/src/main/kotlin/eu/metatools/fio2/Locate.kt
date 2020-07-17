package eu.metatools.fio2

import com.badlogic.gdx.graphics.glutils.ShaderProgram
import java.util.*

private val locateUniform = WeakHashMap<ShaderProgram, WeakHashMap<String, Int>>()

fun ShaderProgram.locateUniform(name: String): Int {
    val cached = locateUniform.getOrPut(this, ::WeakHashMap)
    return cached.getOrPut(name) { getUniformLocation(name) }
}

private val locateAttribute = WeakHashMap<ShaderProgram, WeakHashMap<String, Int>>()

fun ShaderProgram.locateAttribute(alias: String): Int {
    val cached = locateAttribute.getOrPut(this, ::WeakHashMap)
    return cached.getOrPut(alias) { getAttributeLocation(alias) }
}