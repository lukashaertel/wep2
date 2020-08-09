package eu.metatools.fio2

/**
 * Todo: how necessary is this, how strongly coupled must this be.
 *
 * Generally, quads, cg and ag will be exposed to generate remote update request and to obtain locations in quad.
 */
class CommitGenerator() {
    var currentMin = Int.MAX_VALUE
        private set
    var currentMax = Int.MIN_VALUE
        private set

    fun touch(shape: Int) {
        currentMin = minOf(shape, currentMin)
        currentMax = maxOf(shape.inc(), currentMax)
    }

    fun isEmpty() =
            currentMax <= currentMin

    fun reset() {
        currentMin = Int.MAX_VALUE
        currentMax = Int.MIN_VALUE
    }
}

fun CommitGenerator.isNotEmpty() =
        !isEmpty()