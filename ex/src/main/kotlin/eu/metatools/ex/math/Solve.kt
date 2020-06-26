package eu.metatools.ex.math

import eu.metatools.fio.data.*
import kotlin.math.sign
import kotlin.math.sqrt

private fun Float.sq() = this * this

/**
 * Triangle distance metric. Squared distance.
 */
fun td(p: Vec, v1: Vec, v2: Vec, v3: Vec): Float {
    val p1 = p - v1
    val p2 = p - v2
    val p3 = p - v3
    // Base values, edges and normal.
    val v21 = v2 - v1
    val v32 = v3 - v2
    val v13 = v1 - v3
    val nor = v21 cross v13
    val face = sign((v21 cross nor) dot p1) +
            sign((v32 cross nor) dot p2) +
            sign((v13 cross nor) dot p3)
    return if (face < 2f)
        minOf(
            (v21 * ((v21 dot p1) / v21.lenSq).coerceIn(0f, 1f) - p1).lenSq,
            (v32 * ((v32 dot p2) / v32.lenSq).coerceIn(0f, 1f) - p2).lenSq,
            (v13 * ((v13 dot p3) / v13.lenSq).coerceIn(0f, 1f) - p3).lenSq
        )
    else (nor dot p1).sq() / nor.lenSq
}


/**
 * Determines spheroid-triangle passage. The spheroid is defined by start [o] and end [o]+[d]. The spheroid has a
 * radius of [h].
 *
 * @param o Spheroid origin.
 * @param d Direction, origin plus this value is the endpoint.
 * @param v1 First vector of the triangle.
 * @param v2 Second vector of the triangle.
 * @param v3 Third vector of the triangle.
 */
fun sp(o: Vec, d: Vec, v1: Vec, v2: Vec, v3: Vec, h: Float, lessThan: Float = Float.MAX_VALUE): Pair<Float, Vec> {
    //        \ T2 /
    //         \ /
    //    S21  V2  S32
    //        /  \
    //       / I  \
    //-----V1-----V3------
    // T1 /   S13   \ T3


    // Origin from the triangles vectors.
    val o1 = o - v1
    val o2 = o - v2
    val o3 = o - v3

    // TODO: Project, if lessThan is less than value minus h, cannot optimize.

    // Base values, edges and normal.
    val v21 = v2 - v1
    val v32 = v3 - v2
    val v13 = v1 - v3
    val nor = v21 cross v13

    /**
     * Distance squared to the triangle, used for later checking general solutions to the primitive intersections
     * against actual containment.
     */
    fun dsq(p: Vec): Float {
        val p1 = p - v1
        val p2 = p - v2
        val p3 = p - v3
        val face = sign((v21 cross nor) dot p1) +
                sign((v32 cross nor) dot p2) +
                sign((v13 cross nor) dot p3)
        return if (face < 2f)
            minOf(
                (v21 * ((v21 dot p1) / v21.lenSq).coerceIn(0f, 1f) - p1).lenSq,
                (v32 * ((v32 dot p2) / v32.lenSq).coerceIn(0f, 1f) - p2).lenSq,
                (v13 * ((v13 dot p3) / v13.lenSq).coerceIn(0f, 1f) - p3).lenSq
            )
        else (nor dot p1).sq() / nor.lenSq
    }

    // Height squared.
    val hsq = h * h

    var solutionT = Float.MAX_VALUE
    var solutionP = Vec.Zero

    fun minimize(it: Float?) {
        if (it != null && it < solutionT && 0f < it) {
            val p = o + d * it
            if (dsq(p) <= hsq) {
                solutionT = it
                solutionP = p
            }
        }
    }

    // Solve tip distances (what parameter t has to be chosen for the line so that the point has a distance of h units).
    minimize(solveSqMin(d.lenSq, 2f * (d dot o1), o1.lenSq - hsq))
    minimize(solveSqMin(d.lenSq, 2f * (d dot o2), o2.lenSq - hsq))
    minimize(solveSqMin(d.lenSq, 2f * (d dot o3), o3.lenSq - hsq))

    // Solve edge distance (where point has distance h to one of the edges.
    val v21sq = v21 * v21
    val g11 = v21sq * d / v21.lenSq - d
    val g12 = v21sq * o1 / v21.lenSq - o1

    val v32sq = v32 * v32
    val g21 = v32sq * d / v32.lenSq - d
    val g22 = v32sq * o2 / v32.lenSq - o2


    val v13sq = v13 * v13
    val g31 = v13sq * d / v13.lenSq - d
    val g32 = v13sq * o3 / v13.lenSq - o3

    minimize(solveSqMin(g11.lenSq, 2f * (g11 dot g12), g12.lenSq - hsq))
    minimize(solveSqMin(g21.lenSq, 2f * (g21 dot g22), g22.lenSq - hsq))
    minimize(solveSqMin(g31.lenSq, 2f * (g31 dot g22), g32.lenSq - hsq))

    // Solve plane distance (flat surface of the triangle extrusion).
    val nd = nor dot d
    val no1 = nor dot o1

    minimize(solveSqMin(nd.sq() / nor.lenSq, 2f * nd * no1 / nor.lenSq, no1.sq() / nor.lenSq - hsq))

    // Return solution
    return solutionT to solutionP
}

/**
 * Solves the quadratic equation `ax^2 + bx + c = 0` for the first or unique solution.
 * @param a Parameter of the equation.
 * @param b Parameter of the equation.
 * @param c Parameter of the equation.
 */
fun solveSqMin(a: Float, b: Float, c: Float): Float? {
    if (a == 0f)
        return null

    val ac = a * c
    val ac4 = ac + ac + ac + ac

    val inner = b * b - ac4
    if (inner < 0f)
        return null

    // First or unique solution
    return (-b - sqrt(inner)) / (a + a)
}