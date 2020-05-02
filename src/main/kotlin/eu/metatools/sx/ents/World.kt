package eu.metatools.sx.ents

import com.badlogic.gdx.graphics.Color
import eu.metatools.fio.data.Mat
import eu.metatools.fio.data.Tri
import eu.metatools.sx.SX
import eu.metatools.sx.data.*
//import eu.metatools.sx.lang.FP
//import eu.metatools.sx.lang.coerceIn
//import eu.metatools.sx.lang.fp
import eu.metatools.sx.process.ProcessAt
import eu.metatools.up.Ent
import eu.metatools.up.Shell
import eu.metatools.up.dsl.set
import eu.metatools.up.dt.Lx
import java.util.*

typealias FP = Float

val Float.Companion.one get() = 1f
val Float.Companion.zero get() = 0f
fun Float.isNotZero() = this != 0f
val Number.fp get() = toFloat()

/**
 * Performs a breadth-first search from [start] via [out], includes nodes in the result if [isResult] is true.
 */
inline fun <N : Comparable<N>> bfs(start: N, isResult: (N) -> Boolean, out: (N) -> Sequence<N>): NavigableSet<N> {
    // Result set and set of seen nodes..
    val result = TreeSet<N>()
    val seen = hashSetOf<N>()

    // Deque of next choices.
    val next = LinkedList<N>().apply { add(start) }

    // While nodes are available.
    while (next.isNotEmpty()) {
        // Get node from deque.
        val at: N = next.removeFirst()

        // Add to seen nodes.
        seen.add(at)

        // Include node if it's a result.
        if (isResult(at))
            result.add(at)

        // Add all out nodes that are not yet seen to the deque.
        for (to in out(at))
            if (to !in seen)
                next.add(to)
    }

    return result
}

// TODO: Flow direction of fluids.

class World(
    shell: Shell, id: Lx, val sx: SX
) : Ent(shell, id) {
    val players by set<Player>()

    val hidden by volume<Boolean>()

    val cells by volume<Unit>()

    val flows by volume<FP>()

    companion object {
        private fun isTeleport(value: FP?) =
            value != null && 0.9f <= value

        private fun isOutlet(value: FP?) = !isTeleport(value)
    }

    // TODO: Is connectedness of processes needed?

    val fall = object : ProcessAt<FP, FP>() {
        override fun merge(first: FP, second: FP) =
            first + second

        // Verfified no overflow.
        override fun computeAt(volume: Volume<FP>, x: Int, y: Int, z: Int, value: FP) = sequence {
            // Not over open cell, skip.
            if (cells.contains(x, y, z.dec()))
                return@sequence

            // If cell below is filled, skip.
            if (volume[x, y, z.dec()] == FP.one)
                return@sequence

            val capacity = FP.one - (volume[x, y, z.dec()] ?: FP.zero)
            val flow = capacity.coerceIn(FP.zero, value.coerceAtLeast(FP.zero))

            // If flow is not zero, yield it.
            if (flow.isNotZero()) {
                yield(Tri(x, y, z) to -flow)
                yield(Tri(x, y, z.dec()) to flow)
            }
        }
    }

    /**
     * // TODO: Allow extension upwards in teleport determination.
     *
     * Select the one with the greatest value, if that is not unique, order uniquely by location.
     */
    private val equalizationAuthority = compareByDescending<Map.Entry<Tri, FP>> {
        it.value
    }.thenBy {
        it.key
    }

    fun equalize(volume: Volume<FP>, local: Tri, targets: SortedSet<Tri>): Sequence<Pair<Tri, FP>> {
        // No targets, skip.
        if (targets.isEmpty())
            return emptySequence()

        // Associate to values for checks and lookups.
        val associated = targets.associateWith { volume[it] ?: FP.zero }

        // If local is not the authority, skip.
        if (associated.maxBy { (_, value) -> value }?.key != local)
            return emptySequence()

        // Get sum for distribution.
        val sum = targets.fold(FP.zero) { sum, at -> sum + associated.getValue(at) }

        // Start building sequence, this is where it's getting serious.
        return sequence {
            // Get per-instance value and initialize remaining values.
            val perInstance = sum / targets.size
            var remaining = sum

            // Yield de-setting, values distributed are absolute.
            for ((at, value) in associated)
                yield(at to -value)

            // Create round-robin iterator.
            var rri = targets.iterator()

            // Repeat while values are to be distributed.
            while (remaining > 0) {
                // If end reached of target iterator, re-create.
                if (!rri.hasNext()) rri = targets.iterator()

                // Get current location.
                val current = rri.next()

                // Get update for this cell. Limit to remaining to not distribute more than available.
                val update = perInstance.coerceAtMost(remaining)

                // Decrease remaining by exactly this value.
                remaining -= update

                // Yield the update.
                yield(current to update)
            }
        }
    }

    val teleport = object : ProcessAt<FP, FP>() {
        override fun merge(first: FP, second: FP) =
            first + second

        override fun computeAt(volume: Volume<FP>, x: Int, y: Int, z: Int, value: FP): Sequence<Pair<Tri, FP>> {
            // http://www.bay12forums.com/smf/index.php?topic=32453.0

            // If not over open cell, skip.
            if (cells.contains(x, y, z.dec()))
                return emptySequence()

            // If cell below is not filled, skip.
            if (!isTeleport(volume[x, y, z.dec()]))
                return emptySequence()

            // Compute out nodes.
            val teleport = bfs(Tri(x, y, z), {
                // TODO: Add all where below is filled.
                it.x == x && it.y == y && it.z == z || it.z <= z && isOutlet(volume[it])
            }) {
                sequence {
                    if (it.x == x && it.y == y && it.z == z)
                        yield(Tri(it.x, it.y, it.z.dec()))
                    // If the cell in question is valid for teleport, continue on it's neigbours.
                    else if (isTeleport(volume[it])) {
                        yield(Tri(it.x.dec(), it.y, it.z))
                        yield(Tri(it.x.inc(), it.y, it.z))
                        yield(Tri(it.x, it.y.dec(), it.z))
                        yield(Tri(it.x, it.y.inc(), it.z))
                        yield(Tri(it.x, it.y, it.z.dec()))
                        yield(Tri(it.x, it.y, it.z.inc()))
                    }
                }.filter {
                    // Cells may not be occupied by solid.
                    !cells.contains(it)
                }
            }

            return equalize(volume, Tri(x, y, z), teleport)
        }
    }

    val distribute = object : ProcessAt<FP, FP>() {
        override fun merge(first: FP, second: FP) =
            first + second

        override fun computeAt(volume: Volume<FP>, x: Int, y: Int, z: Int, value: FP) = sequence {
            // Not over closed or filled cell, skip.
            if (!cells.contains(x, y, z.dec()) && volume[x, y, z.dec()] != FP.one)
                return@sequence
            val freeLeft = !cells.contains(x.dec(), y, z)
            val freeRight = !cells.contains(x.inc(), y, z)
            val freeFront = !cells.contains(x, y.inc(), z)
            val freeBack = !cells.contains(x, y.dec(), z)

            val targets = (if (freeLeft) 1 else 0) +
                    (if (freeRight) 1 else 0) +
                    (if (freeFront) 1 else 0) +
                    (if (freeBack) 1 else 0)

            if (targets == 0)
                return@sequence

            val atLeft = (volume[x.dec(), y, z] ?: FP.zero)
            val atRight = (volume[x.inc(), y, z] ?: FP.zero)
            val atFront = (volume[x, y.inc(), z] ?: FP.zero)
            val atBack = (volume[x, y.dec(), z] ?: FP.zero)

            val sumOther = atLeft + atRight + atFront + atBack
            val sumTotal = value + sumOther
            val distributeOther = sumTotal / targets.inc().fp
            val distributeSelf = sumTotal - distributeOther * targets.fp

            // Do not fill inward, only push out.
            if (distributeSelf >= value)
                return@sequence

            // Get flow of own cell, update to new value.
            val flowSelf = distributeSelf - value
            yield(Tri(x, y, z) to flowSelf)

            if (freeLeft) {
                val flowLeft = distributeOther - atLeft
                if (flowLeft.isNotZero())
                    yield(Tri(x.dec(), y, z) to flowLeft)
            }

            if (freeRight) {
                val flowRight = distributeOther - atRight
                if (flowRight.isNotZero())
                    yield(Tri(x.inc(), y, z) to flowRight)
            }

            if (freeFront) {
                val flowFront = distributeOther - atFront
                if (flowFront.isNotZero())
                    yield(Tri(x, y.inc(), z) to flowFront)
            }

            if (freeBack) {
                val flowBack = distributeOther - atBack
                if (flowBack.isNotZero())
                    yield(Tri(x, y.dec(), z) to flowBack)
            }
        }
    }

    private fun mergeFlows(first: FP?, second: FP) =
        (if (first == null) second else first + second).takeIf(FP::isNotZero)

    private fun checkNonExceed(updates: SortedMap<Tri, FP>) {
        val exceed = updates.filter { (at, delta) -> (flows[at] ?: FP.zero) + delta < FP.zero }
        check(exceed.isEmpty()) {
            "$exceed exceed boundaries."
        }

    }

    /**
     * Periodic world update.
     */
    val worldUpdate = repeating(Short.MAX_VALUE, 40, shell::initializedTime) {

        val distributeFlow = distribute.compute(flows)
        flows.merge(distributeFlow, ::mergeFlows)

        val fallFLow = fall.compute(flows)
        flows.merge(fallFLow, ::mergeFlows)

        val teleportFlow = teleport.compute(flows)
        flows.merge(teleportFlow, ::mergeFlows)


//        flows.merge(legacyProc.compute(flows)) { ex, delta ->
//            ((ex ?: defaultFlow) + delta).takeIf { it.mass.isNotZero() }
//        }


        // flows.getAll().toMap().let(::println)
        // fluid.getAll().sumBy { it.second.mass }.let(::println)

        // Remove outside of bounds.
        val oob = flows[Int.MIN_VALUE..Int.MAX_VALUE, Int.MIN_VALUE..Int.MAX_VALUE, Int.MIN_VALUE..-20].associate {
            it.first to null
        }
        flows.assign(oob)
    }

    val add = exchange(::doAdd)

    private fun doAdd(coord: Tri) {
        if (coord !in cells)
            flows[coord.x, coord.y, coord.z] = FP.one
    }

    /**
     * Renders all actors.
     */
    fun render(time: Double, delta: Double) {
        cells.getAll().forEach { (at, _) ->
            if (hidden[at.x, at.y, at.z] != true) // TODO
                sx.cube(
                    at.copy(z = at.z.inc()) to Tri.Zero, Color.WHITE,
                    Mat.translation(at.x * 4f, at.z * 4f, at.y * 4f).scale(4f, 4f, 4f)
                )
        }
        flows.getAll().forEach { (at, value) ->
            if (0.01.fp < value) {
                sx.cube(
                    at to value,
                    Color(0f, 1f, 1f, 0.4f),
                    Mat.translation(at.x * 4f, at.z * 4f, at.y * 4f)
                        .scale(4f, 4f, 4f)
                        .translate(0f, -0.5f, 0f)
                        .scale(sy = value.toFloat())
                        .translate(0f, 0.5f, 0f)
                )
            }
        }
    }
}