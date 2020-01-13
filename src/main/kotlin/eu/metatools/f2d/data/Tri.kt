package eu.metatools.f2d.data

// TODO: Align with quality of remaining data objects.

data class Tri(val x: Int, val y: Int, val z: Int) : Comparable<Tri> {
    val isEmpty get() = x == 0 && y == 0 && z == 0

    override fun compareTo(other: Tri): Int {
        val rz = z.compareTo(other.z)
        if (rz != 0) return rz
        val ry = -y.compareTo(other.y)
        if (ry != 0) return ry
        return x.compareTo(other.x)
    }

    override fun toString() = "($x, $y, $z)"
}