package eu.metatools.fio2


/**
 * Target handling draw of shapes.
 */
interface DrawsTarget {
    /**
     * Target vertex data.
     */
    val vertices: BindVertices
}

interface Draws {
    fun draw(time: Double, target: DrawsTarget)
}