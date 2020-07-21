package eu.metatools.fio2

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.NumberUtils
import eu.metatools.fio.data.Col
import eu.metatools.fio2.QuadsConstants.attributes
import eu.metatools.fio2.QuadsConstants.bytesBetweenPositions
import eu.metatools.fio2.QuadsConstants.bytesPerShape
import eu.metatools.fio2.QuadsConstants.floatsPerShape
import eu.metatools.fio2.QuadsConstants.indicesPerShape
import eu.metatools.fio2.QuadsConstants.zeroVertex

object QuadsConstants {
    /**
     * Vertex attributes to render the shape.
     */
    val attributes = VertexAttributes(
            VertexAttribute(VertexAttributes.Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE),
            VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE),
            VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0")
    )

    /**
     * Six indices are used per shape.
     */
    const val indicesPerShape = 6

    /**
     * Three floats are used for position.
     */
    const val floatsForPosition = 3

    /**
     * One float is used for packed color.
     */
    const val floatsForColor = 1

    /**
     * Two floats are used for the UV coordinates.
     */
    const val floatsForUV = 2

    /**
     * Number of floats per vertex.
     */
    const val floatsPerVertex = floatsForPosition + floatsForColor + floatsForUV

    /**
     * Number of bytes used for position.
     */
    const val bytesForPosition = floatsForPosition * 4

    /**
     * Number of bytes per vertex.
     */
    const val bytesPerVertex = floatsPerVertex * 4

    /**
     * The amount of floats per shape.
     */
    const val floatsPerShape = floatsPerVertex * 4

    /**
     * The amount of bytes per shape.
     */
    const val bytesPerShape = bytesPerVertex * 4

    /**
     * Bytes between end of first and start of next position.
     */
    const val bytesBetweenPositions = bytesPerVertex - bytesForPosition

    val zeroVertex = ByteArray(bytesPerVertex)
}

/**
 * Single vertex building pass.
 * @param quad True if quad shapes to be used.
 * @param limit Maximum number of shapes.
 */
class Quads(val limit: Int) : DrawsTarget, AutoCloseable {
    companion object {
        private const val isStatic = false
    }

    /**
     * True if VAO is used.
     */
    private val vao = Gdx.gl30 != null

    /**
     * True if indices are short.
     */
    private val short = limit * indicesPerShape < Short.MAX_VALUE

    /**
     * OpenGL Type of indices used.
     */
    private val indicesType = if (short) GL20.GL_UNSIGNED_SHORT else GL20.GL_UNSIGNED_INT

    /**
     * Vertex buffer. Direct.
     */
    override val vertices = if (vao)
        VerticesVAO(isStatic, attributes, limit * floatsPerShape)
    else
        VerticesArray(attributes, limit * floatsPerShape)

    /**
     * Index buffer. Direct.
     */
    private val indices = if (vao) {
        IndexVAO(isStatic, short, limit * indicesPerShape)
    } else {
        IndexArray(short, limit * indicesPerShape)
    }

    init {
        // Set buffer limits according to their size.
        vertices.buffer.clear()
        indices.buffer.clear()

        // Get running vertex index.
        var at = 0

        // Disambiguate on maximum count.
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

        // Flip buffer data.
        indices.buffer.flip()

        // Commit data.
        indices.bind()
        indices.commit()
        indices.unbind()
    }

    /**
     * Positions the vertex buffer source at the start of quad number [start].
     */
    fun position(start: Int = 0) {
        vertices.position(start * 4)
    }

    /**
     * Commits quads from the source to the vertex buffer. If given, [start] and [end] give the range of shapes to
     * be committed.
     */
    fun commit(start: Int = 0, end: Int? = null) {
        vertices.bind()
        vertices.commit(start * 4, end?.times(4))
        vertices.unbind()
    }

    override fun close() {
        vertices.close()
        indices.close()
    }

    /**
     * Renders all shapes up to the [count]th shape.
     */
    fun render(shader: ShaderProgram, count: Int) {
        // Attach vertices and shader.
        val withShader = vertices.on(shader)

        // Bind vertices attached to shader and indices.
        withShader.bind()
        indices.bind()

        // Draw buffer or bound VAO.
        if (vao)
            Gdx.gl20.glDrawElements(GL20.GL_TRIANGLES, count * indicesPerShape, indicesType, 0)
        else
            Gdx.gl20.glDrawElements(GL20.GL_TRIANGLES, count * indicesPerShape, indicesType, indices.buffer)

        // Unbind data.
        indices.unbind()
        withShader.unbind()
    }
}

/**
 * Sets an empty vertex at the current position.
 */
@Suppress("nothing_to_inline")
inline fun Quads.vertexEmpty() {
    vertices.buffer.apply {
        put(zeroVertex)
    }
}

/**
 * Sets a vertex at the current position.
 */
@Suppress("nothing_to_inline")
inline fun Quads.vertex(x: Float, y: Float, z: Float, color: Float, u: Float, v: Float) {
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
 * Sets a vertex at the current position. Converts the [color].
 */
@Suppress("nothing_to_inline")
inline fun Quads.vertex(x: Float, y: Float, z: Float, color: Col, u: Float, v: Float) {
    vertices.buffer.apply {
        putFloat(x)
        putFloat(y)
        putFloat(z)
        putFloat(NumberUtils.intToFloatColor(color.packed))
        putFloat(u)
        putFloat(v)
    }
}


/**
 * Sorts the underlying buffer in place based on the central Z-distance of the quads. Coordinates with a greater dot
 * product with the axis will be placed later in the buffer.
 *
 * Important: the underlying structure is reordered, therefore managed indices will not appropriately work. Use for
 * streamed push and transparency sort.
 *
 * @param start The index of the first shape to include in sort.
 * @param end The index of last shape to include in sort.
 * @param axisX The X-axis for sorting.
 * @param axisY The Y-axis for sorting.
 * @param axisZ The Z-axis for sorting.
 */
fun Quads.sortQuad(start: Int, end: Int, axisX: Float, axisY: Float, axisZ: Float) {
    // Get buffer and start position.
    val buffer = vertices.buffer
    val posBefore = buffer.position()

    /**
     * Gets the key for the vertex at the address.
     */
    fun key(address: Int): Float {
        buffer.position(address)
        val x0 = buffer.float
        val y0 = buffer.float
        val z0 = buffer.float
        buffer.position(buffer.position() + bytesBetweenPositions)
        val x1 = buffer.float
        val y1 = buffer.float
        val z1 = buffer.float
        buffer.position(buffer.position() + bytesBetweenPositions)
        val x2 = buffer.float
        val y2 = buffer.float
        val z2 = buffer.float
        buffer.position(buffer.position() + bytesBetweenPositions)
        val x3 = buffer.float
        val y3 = buffer.float
        val z3 = buffer.float

        // Central dot product.
        return (x0 + x1 + x2 + x3) * axisX +
                (y0 + y1 + y2 + y3) * axisY +
                (z0 + z1 + z2 + z3) * axisZ
    }

    // Cache array for shifting.
    val cacheFirst = ByteArray(bytesPerShape)
    val cacheShift = ByteArray(bytesPerShape)

    /**
     * Performs right rotation for the data in the address range.
     */
    fun rotate(addressStart: Int, addressEnd: Int) {
        // Move to end address, get data there.
        buffer.position(addressEnd)
        buffer.get(cacheFirst)

        // Position at source for shift copy from.
        buffer.position(addressEnd - bytesPerShape)

        // While not before start, shift.
        while (true) {
            // Get current location.
            val source = buffer.position()

            // Get shape value and put after it.
            buffer.get(cacheShift)
            buffer.put(cacheShift)

            // If this run was at start, stop loop.
            if (source == addressStart)
                break

            // Move back before previous element.
            buffer.position(source - bytesPerShape)
        }

        // Put the end vertex at the new start.
        buffer.position(addressStart)
        buffer.put(cacheFirst)
    }

    /**
     * Merge step for the given data.
     */
    fun merge(addressStart: Int, addressMid: Int, addressEnd: Int) {
        var mergeStart = addressStart
        var mergeMid = addressMid
        var mergeStartSnd = mergeMid + bytesPerShape

        if (key(mergeMid) <= key(mergeStartSnd))
            return

        while (mergeStart <= mergeMid && mergeStartSnd <= addressEnd) {
            if (key(mergeStart) <= key(mergeStartSnd)) {
                mergeStart += bytesPerShape
            } else {
                rotate(mergeStart, mergeStartSnd)
                mergeStart += bytesPerShape
                mergeMid += bytesPerShape
                mergeStartSnd += bytesPerShape
            }
        }
    }

    /**
     * Recursive merge sort function.
     */
    fun mergeSort(elementLeft: Int, elementRight: Int) {
        // Check ordering.
        if (elementLeft < elementRight) {
            // Get mid element.
            val elementMid = elementLeft + (elementRight - elementLeft) / 2

            // Recursion.
            mergeSort(elementLeft, elementMid)
            mergeSort(elementMid + 1, elementRight)

            // Merging.
            merge(elementLeft * bytesPerShape, elementMid * bytesPerShape, elementRight * bytesPerShape)
        }
    }

    // Sort entire buffer.
    mergeSort(start, end)

    // Reset position.
    buffer.position(posBefore)
}