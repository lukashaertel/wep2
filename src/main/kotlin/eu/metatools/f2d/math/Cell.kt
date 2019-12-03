package eu.metatools.f2d.math

// TODO: Align with quality of remaining data objects.

data class Cell(val x: Int, val y: Int) : Comparable<Cell> {
    val isEmpty get() = x == 0 && y == 0

    override fun compareTo(other: Cell): Int {
        val r = y.compareTo(other.y)
        if (r != 0) return r
        return x.compareTo(other.x)
    }

    override fun toString() = "($x, $y)"
}
