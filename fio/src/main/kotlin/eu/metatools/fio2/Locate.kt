package eu.metatools.fio2

import com.badlogic.gdx.graphics.glutils.ShaderProgram
import java.util.*

/**
 * Association from shader program over uniform name to location.
 */
private val locateUniform = WeakHashMap<ShaderProgram, WeakHashMap<String, Int>>()

/**
 * Gets a shader program's uniform location by name.
 */
fun ShaderProgram.locateUniform(name: String): Int {
    val cached = locateUniform.getOrPut(this, ::WeakHashMap)
    return cached.getOrPut(name) { getUniformLocation(name) }
}

/**
 * Association from shader program over attribute name to location.
 */
private val locateAttribute = WeakHashMap<ShaderProgram, WeakHashMap<String, Int>>()

/**
 * Gets a shader program's attribute location by alias.
 */
fun ShaderProgram.locateAttribute(alias: String): Int {
    val cached = locateAttribute.getOrPut(this, ::WeakHashMap)
    return cached.getOrPut(alias) { getAttributeLocation(alias) }
}