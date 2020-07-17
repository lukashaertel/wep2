package eu.metatools.fio2

/**
 * Size of a vertex of a shape.
 */
const val SHAPE_VERTEX_SIZE = 3 + 1 + 2

/**
 * Target handling draw of shapes.
 */
interface DrawsTarget {
    /**
     * Target vertex data.
     */
    val vertices: BindVertices

    /**
     * True if quads are drawn.
     */
    val quad: Boolean
}

interface Draws {
    fun draw(time: Double, target: DrawsTarget)
}