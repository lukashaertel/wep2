package eu.metatools.fio.immediate

import eu.metatools.fio.Timed
import eu.metatools.fio.end
import eu.metatools.fio.data.Mat
import eu.metatools.fio.data.Vec
import eu.metatools.fio.data.Vecs
import eu.metatools.fio.data.reduceComponents
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Provides methods for validating calls on timed objects and objects with centers.
 */
abstract class ProjectionTrimmed(val trimExcess: Float) {
    /**
     * Vectors that are [trimExcess] outside the uniform projection space.
     */
    private val excessVectors = Vecs(8) {
        Vec(
            if ((it / 4) % 2 == 0) -1f - trimExcess else 1f + trimExcess,
            if ((it / 2) % 2 == 0) -1f - trimExcess else 1f + trimExcess,
            if ((it / 1) % 2 == 0) -1f - trimExcess else 1f + trimExcess
        )
    }

    /**
     * Value of the projection matrix, initialized to [Mat.ID].
     */
    private var projectionValue: Mat = Mat.ID

    /**
     * The projection matrix to use.
     */
    protected var projection
        get() = projectionValue
        set(value) {
            // Do not update on identity.
            if (projectionValue == value)
                return

            // Transfer matrix.
            projectionValue = value

            // Setup bounds.
            excessMin = Vec.PositiveInfinity
            excessMax = Vec.NegativeInfinity

            // Reduce by minimum/maximum respectively.
            for (v in projection.inv * excessVectors) {
                excessMin = reduceComponents(excessMin, v, ::minOf)
                excessMax = reduceComponents(excessMax, v, ::maxOf)
            }
        }

    /**
     * The minimum in-bounds vector. Computed on [projection] assignment.
     */
    var excessMin = Vec(-1f - trimExcess, -1f - trimExcess, -1f - trimExcess)
        private set

    /**
     * The maximum in-bounds vector. Computed on [projection] assignment.
     */
    var excessMax = Vec(1f + trimExcess, 1f + trimExcess, 1f + trimExcess)
        private set

}

/**
 * Validates time, does not perform boundary check.
 */
@Suppress("experimental_api_usage_error")
inline fun ProjectionTrimmed.validate(timed: Timed, time: Double, block: () -> Unit) {
    contract { callsInPlace(block, InvocationKind.AT_MOST_ONCE) }

    // Not within time frame, return.
    if (time < timed.start) return
    if (timed.end <= time) return

    // Valid, run block.
    block()
}

/**
 * Validates time and performs boundary check.
 */
@Suppress("experimental_api_usage_error")
inline fun ProjectionTrimmed.validate(timed: Timed, time: Double, transform: Mat, block: () -> Unit) {
    contract { callsInPlace(block, InvocationKind.AT_MOST_ONCE) }

    // Not within time frame, return.
    if (time < timed.start) return
    if (timed.end <= time) return

    // Not within visible portion, return.
    val (x, y, z) = transform.center

    if (x < excessMin.x) return
    if (y < excessMin.y) return
    if (z < excessMin.z) return
    if (excessMax.x < x) return
    if (excessMax.y < y) return
    if (excessMax.z < z) return

    // Valid, run block.
    block()
}