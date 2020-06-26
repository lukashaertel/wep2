package eu.metatools.fio.immediate

import com.badlogic.gdx.math.Frustum
import eu.metatools.fio.Timed
import eu.metatools.fio.data.Mat
import eu.metatools.fio.data.Vec
import eu.metatools.fio.end
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Provides methods for validating calls on timed objects and objects with centers.
 */
abstract class ProjectionTrimmed(val radiusLimit: Float) {
    /**
     * Frustum defining the viewed area.
     */
    private val frustum = Frustum()

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

            // Update frustum.
            frustum.update(projection.inv.asMatrix())
        }

    /**
     * True if the point is contained with the radius of [radiusLimit].
     */
    operator fun contains(vec: Vec) =
        frustum.sphereInFrustum(vec.x, vec.y, vec.z, radiusLimit)
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

    // Not contained in visible area, return.
    if (transform.center !in this) return

    // Valid, run block.
    block()
}