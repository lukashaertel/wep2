package eu.metatools.fio2

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.NumberUtils
import eu.metatools.fio.data.Col

/**
 * Vertex attributes to render the shape.
 */
private val shapeAttributes = VertexAttributes(
        VertexAttribute(VertexAttributes.Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE),
        VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE),
        VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0")
)

/**
 * Single vertex building pass.
 * @param quad True if quad shapes to be used.
 * @param limit Maximum number of shapes.
 */
class ShapeTarget(override val quad: Boolean, val limit: Int) : DrawsTarget, AutoCloseable {
    companion object {
        private const val isStatic = false
    }

    /**
     * Six for [quad]s, three for triangles.
     */
    private val indicesPerShape = if (quad) 6 else 3

    /**
     * The amount of floats per shape.
     */
    private val floatsPerShape = if (quad) 4 * SHAPE_VERTEX_SIZE else 3 * SHAPE_VERTEX_SIZE

    /**
     * True if VAO is used.
     */
    private val vao = Gdx.gl30 != null

    private val short = limit * indicesPerShape < Short.MAX_VALUE

    /**
     * Vertex buffer. Direct.
     */
    override val vertices = if (vao)
        VerticesVAO(isStatic, shapeAttributes, limit * floatsPerShape)
    else
        VerticesArray(shapeAttributes, limit * floatsPerShape)

    /**
     * Index buffer. Direct.
     */
    private val indices = if (vao) {
        IndexVAO(isStatic, short, limit * indicesPerShape)
    } else {
        IndexArray(short, limit * indicesPerShape)
    }

    private val indicesType = if (short) GL20.GL_UNSIGNED_SHORT else GL20.GL_UNSIGNED_INT

    init {
        // Prepare for writing.
        indices.buffer.clear()

        // Get running vertex index.
        var at = 0

        // Disambiguate on is-quad/is-short basis.
        if (quad) {
            if (short)
                repeat(limit) {
                    // Put all short quad indices.
                    indices.buffer.putShort((at + 0).toShort())
                    indices.buffer.putShort((at + 1).toShort())
                    indices.buffer.putShort((at + 2).toShort())
                    indices.buffer.putShort((at + 0).toShort())
                    indices.buffer.putShort((at + 2).toShort())
                    indices.buffer.putShort((at + 3).toShort())
                    at += 4
                }
            else
                repeat(limit) {
                    // Put all int quad indices.
                    indices.buffer.putInt(at + 0)
                    indices.buffer.putInt(at + 1)
                    indices.buffer.putInt(at + 2)
                    indices.buffer.putInt(at + 0)
                    indices.buffer.putInt(at + 2)
                    indices.buffer.putInt(at + 3)
                    at += 4
                }
        } else {
            if (short)
                repeat(limit) {
                    // Put all short tri indices.
                    indices.buffer.putShort((at + 0).toShort())
                    indices.buffer.putShort((at + 1).toShort())
                    indices.buffer.putShort((at + 2).toShort())
                    at += 3
                }
            else
                repeat(limit) {
                    // Put all int tri indices.
                    indices.buffer.putInt(at + 0)
                    indices.buffer.putInt(at + 1)
                    indices.buffer.putInt(at + 2)
                    at += 3
                }
        }

        // Flip buffer data.
        indices.buffer.flip()

        // Commit data.
        indices.bind()
        indices.commit()
        indices.unbind()
    }

    fun beginData() {
        vertices.bind()
        vertices.buffer.clear()
    }

    fun endData() {
        vertices.buffer.flip()
        vertices.commit()
        vertices.unbind()
    }

    override fun close() {
        vertices.close()
        indices.close()
    }

    fun render(shader: ShaderProgram) {
        val withShader = vertices.on(shader)

        withShader.bind()
        indices.bind()

        val count = (vertices.buffer.limit() / shapeAttributes.vertexSize) / if (quad) 4 else 3
        if (vao)
            Gdx.gl20.glDrawElements(GL20.GL_TRIANGLES, count * indicesPerShape, indicesType, 0)
        else
            Gdx.gl20.glDrawElements(GL20.GL_TRIANGLES, count * indicesPerShape, indicesType, indices.buffer)

        indices.unbind()
        withShader.unbind()
    }
}

/**
 * Adds a vertex.
 */
@Suppress("nothing_to_inline")
inline fun ShapeTarget.addVertex(x: Float, y: Float, z: Float, color: Float, u: Float, v: Float) {
    vertices.buffer.apply {
        putFloat(x)
        putFloat(y)
        putFloat(z)
        putFloat(color)
        putFloat(u)
        putFloat(v)
    }
}

/**
 * Adds a vertex.
 */
@Suppress("nothing_to_inline")
inline fun ShapeTarget.addVertex(x: Float, y: Float, z: Float, color: Col, u: Float, v: Float) {
    vertices.buffer.apply {
        putFloat(x)
        putFloat(y)
        putFloat(z)
        putFloat(NumberUtils.intToFloatColor(color.packed))
        putFloat(u)
        putFloat(v)
    }
}


fun ShapeTarget.sortQuad(axisX: Float, axisY: Float, axisZ: Float) {
    // Get buffer and start position.
    val buffer = vertices.buffer
    val posBefore = buffer.position()

    // Jump between vertices and between end of position to next start of position.
    val shape = shapeAttributes.vertexSize * 4
    val nextVec = shapeAttributes.vertexSize - 12

    /**
     * Gets the key for the vertex at the address.
     */
    fun key(address: Int): Float {
        buffer.position(address)
        val x0 = buffer.float
        val y0 = buffer.float
        val z0 = buffer.float
        buffer.position(buffer.position() + nextVec)
        val x1 = buffer.float
        val y1 = buffer.float
        val z1 = buffer.float
        buffer.position(buffer.position() + nextVec)
        val x2 = buffer.float
        val y2 = buffer.float
        val z2 = buffer.float
        buffer.position(buffer.position() + nextVec)
        val x3 = buffer.float
        val y3 = buffer.float
        val z3 = buffer.float

        return (x0 + x1 + x2 + x3) * axisX +
                (y0 + y1 + y2 + y3) * axisY +
                (z0 + z1 + z2 + z3) * axisZ
    }

    // Cache array for shifting.
    val cacheFirst = ByteArray(shape)
    val cacheShift = ByteArray(shape)

    /**
     * Performs right shift for all data in the address range.
     */
    fun shiftRight(addressStart: Int, addressEnd: Int) {
        // Move to end address, get data there.
        buffer.position(addressEnd)
        buffer.get(cacheFirst)

        // Position at source for shift copy from.
        buffer.position(addressEnd - shape)

        // While not at start, shift.
        while (true) {
            // Get shape value and put after it.
            buffer.get(cacheShift)
            buffer.put(cacheShift)

            // Compute next start offset, do not go before the address.
            val addressNext = buffer.position() - shape - shape - shape
            if (addressNext < addressStart)
                break

            // Move before source shape.
            buffer.position(addressNext)
        }

        // Put the end vertex at the new start.
        buffer.position(addressStart)
        buffer.put(cacheFirst)
    }

    fun merge(addressStart: Int, addressMid: Int, addressEnd: Int) {
        var start = addressStart
        var mid = addressMid
        var start2 = mid + shape

        if (key(mid) <= key(start2))
            return

        while (start <= mid && start2 <= addressEnd) {
            if (key(start) <= key(start2)) {
                start += shape
            } else {
                shiftRight(start, start2)
                start += shape
                mid += shape
                start2 += shape
            }
        }
    }

    fun mergeSort(elementLeft: Int, elementRight: Int) {
        // Check ordering.
        if (elementLeft < elementRight) {
            // Get mid element.
            val elementMid = elementLeft + (elementRight - elementLeft) / 2

            // Recursion.
            mergeSort(elementLeft, elementMid)
            mergeSort(elementMid + 1, elementRight)

            // Merging.
            merge(elementLeft * shape, elementMid * shape, elementRight * shape)
        }
    }

    // Sort entire buffer.
    mergeSort(0, posBefore / shape)

    // Reset position.
    buffer.position(posBefore)
}