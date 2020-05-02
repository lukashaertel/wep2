package eu.metatools.sx.ents

import eu.metatools.fio.data.Tri
import eu.metatools.sx.lang.FP
import eu.metatools.sx.lang.coerceAtLeast
import eu.metatools.sx.lang.coerceAtMost

/**
 * Flow definition.
 * @property mass The mass currently in the cell. Might be compressed as per [Cell.capacity].
 * @property left The flow to the left. Positive values mean in-flow.
 * @property right The flow to the right. Positive values mean in-flow.
 * @property front The flow to the front. Positive values mean in-flow.
 * @property back The flow to the back. Positive values mean in-flow.
 * @property above The flow to the above. Positive values mean in-flow.
 * @property below The flow to the below. Positive values mean in-flow.
 */
data class Flow(
    val mass: FP,
    val left: FP = FP.zero,
    val right: FP = FP.zero,
    val front: FP = FP.zero,
    val back: FP = FP.zero,
    val above: FP = FP.zero,
    val below: FP = FP.zero
) {
    /**
     * Negates all properties.
     */
    operator fun unaryMinus() =
        Flow(-mass, -left, -right, -front, -back, -above, -below)

    /**
     * Adds the properties.
     */
    operator fun plus(other: Flow) =
        Flow(
            mass + other.mass,
            left + other.left,
            right + other.right,
            front + other.front,
            back + other.back,
            above + other.above,
            below + other.below
        )

    /**
     * True if any flow of [left], [right], [front] or [back] is not zero.
     */
    fun hasHorizontalFlow() =
        left.isNotZero() || right.isNotZero() || front.isNotZero() || back.isNotZero()

    /**
     * True if [above] or [below] is not negative.
     */
    fun hasVerticalFlow() =
        above.isNotZero() || below.isNotZero()

    /**
     * Returns the flow sum of the corresponding flows.
     */
    val cardinalDirection by lazy {
        Tri(
            (left + right).toInt(),
            (front + back).toInt(),
            (above + below).toInt()
        )
    }

    /**
     * Gets a copy of the [Flow] without mass.
     */
    val dynamics get() = copy(mass = FP.zero)

    /**
     * Sum of all positive flows.
     */
    val inFlow by lazy {
        left.coerceAtLeast(FP.zero) +
                right.coerceAtLeast(FP.zero) +
                front.coerceAtLeast(FP.zero) +
                back.coerceAtLeast(FP.zero) +
                above.coerceAtLeast(FP.zero) +
                below.coerceAtLeast(FP.zero)
    }

    /**
     * Sum of all negative flows.
     */
    val outFlow by lazy {
        -left.coerceAtMost(FP.zero) -
                right.coerceAtMost(FP.zero) -
                front.coerceAtMost(FP.zero) -
                back.coerceAtMost(FP.zero) -
                above.coerceAtMost(FP.zero) -
                below.coerceAtMost(FP.zero)
    }
}