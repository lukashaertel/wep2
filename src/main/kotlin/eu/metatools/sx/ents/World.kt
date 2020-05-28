package eu.metatools.sx.ents

import com.badlogic.gdx.graphics.Color
import eu.metatools.fio.data.*
import eu.metatools.fio.drawable.color
import eu.metatools.fio.resource.get
import eu.metatools.sx.SX
import eu.metatools.sx.util.findFirstPath
import eu.metatools.sx.util.flatAssociate
import eu.metatools.sx.util.flex
import eu.metatools.up.Ent
import eu.metatools.up.Shell
import eu.metatools.up.dsl.map
import eu.metatools.up.dsl.set
import eu.metatools.up.dt.Lx
import kotlinx.coroutines.runBlocking
import java.util.*

fun <K, V : Any> NavigableMap<K, V>.mergeAll(merge: (V, V) -> V?, other: Sequence<Pair<K, V>>) {
    // Make change sets.
    val remove = TreeSet(comparator())
    val put = TreeMap<K, V>(comparator())

    // Iterate all for merged results.
    for ((k, v) in other) {
        // Get existing value.
        val existing = get(k)

        // Check if existed.
        if (existing == null) {
            // Did not exist, just put.
            put[k] = v
        } else {
            // Compute from merge.
            val update = merge(existing, v)

            // If merge is now null, remove, otherwise put.
            if (update == null)
                remove.add(k)
            else
                put[k] = update
        }
    }

    // Update map.
    keys.removeAll(remove)
    putAll(put)
}

fun Float.plusNotZero(other: Float) = plus(other).takeIf { it != 0f }

fun Flow.plusNotZero(other: Flow) = plus(other).takeIf(Flow::isNotEmpty)

interface CellType {
    companion object {
        val none = object : CellType {
            override val flowRate = 1f
            override val fluidLimit = 1f
            override val fluidDirs = Dirs.all
            override fun toString() = "(none)"
        }
    }

    /**
     * The (in-)flow rate for this cell.
     */
    val flowRate: Float

    /**
     * Defines how much fluid can this cell take.
     */
    val fluidLimit: Float

    /**
     * Defines in which direction the fluid can flow.
     */
    val fluidDirs: Dirs
}

enum class CellTypes(
    override val flowRate: Float = 1f,
    override val fluidLimit: Float = 1f,
    override val fluidDirs: Dirs = Dirs.all
) : CellType {
    Rock(fluidLimit = 0f),
    Soil(0.1f, 0.1f),
    PipeX(fluidDirs = Dirs.xs),
    PipeY(fluidDirs = Dirs.ys),
    PipeZ(fluidDirs = Dirs.zs)
}

data class Flow(
    val left: Float, val right: Float,
    val back: Float, val front: Float,
    val under: Float, val over: Float
) {
    companion object {
        val Zero = Flow(0f, 0f, 0f, 0f, 0f, 0f)
        val One = Flow(0f, 0f, 0f, 0f, 0f, 0f)
        fun left(value: Float) = Flow(value, 0f, 0f, 0f, 0f, 0f)
        fun right(value: Float) = Flow(0f, value, 0f, 0f, 0f, 0f)
        fun back(value: Float) = Flow(0f, 0f, value, 0f, 0f, 0f)
        fun front(value: Float) = Flow(0f, 0f, 0f, value, 0f, 0f)
        fun under(value: Float) = Flow(0f, 0f, 0f, 0f, value, 0f)
        fun over(value: Float) = Flow(0f, 0f, 0f, 0f, 0f, value)
    }

    operator fun plus(other: Flow) = Flow(
        left + other.left,
        right + other.right,
        back + other.back,
        front + other.front,
        under + other.under,
        over + other.over
    )

    fun isEmpty() =
        left == 0f &&
                right == 0f &&
                back == 0f &&
                front == 0f &&
                under == 0f &&
                over == 0f
}

// TODO: Inlets and outlets that can manually transform flow.

fun Flow.isNotEmpty() = !isEmpty()

class World(
    shell: Shell, id: Lx, val sx: SX
) : Ent(shell, id) {
    val players by set<Player>()

    val hidden by set<Tri>()

    /**
     * Type of cell per location.
     */
    val types by map<Tri, CellType>()

    /**
     * Level of fluid per location.
     */
    val level by map<Tri, Float>()

    /**
     * Flow per location.
     */
    val flows by map<Tri, Flow>()

    companion object {
        /**
         * Milliseconds for update.
         */
        const val millis = 100L

        /**
         * Delta-time for the update.
         */
        val dt = millis / 1000f

        val fps = 10f * dt
    }

    // TODO: Inlets, allowing solid blocks to receive fluid.

    /**
     * Returns true if [fromType] can have fluid leave via [dir] and [toType] can have fluid enter from the
     * opposite [dir] ([Dirs.unaryMinus]).
     */
    private fun canTravel(fromType: CellType, toType: CellType, dir: Dirs) =
        0f < toType.fluidLimit
                && dir in fromType.fluidDirs
                && -dir in toType.fluidDirs

    /**
     * Returns true if level at [at] is equal (or greater) than the [CellType.fluidLimit] of the [atType].
     */
    private fun isFilled(at: Tri, atType: CellType) =
        atType.fluidLimit <= level[at] ?: 0f

    /**
     * Adds the components.
     */
    private fun plusPairs(a: Pair<Float, Flow>, b: Pair<Float, Flow>) =
        (a.first + b.first) to (a.second + b.second)

    /**
     * Creates a [Flow] with the [value] in the given direction.
     */
    private fun Dirs.toFlow(value: Float) =
        when (this) {
            Dirs.left -> Flow.left(value)
            Dirs.right -> Flow.right(value)
            Dirs.back -> Flow.back(value)
            Dirs.front -> Flow.front(value)
            Dirs.under -> Flow.under(value)
            Dirs.over -> Flow.over(value)
            else -> error("Undefined.")
        }

    /**
     * Resets all flows.
     */
    private fun resetFlows() {
        flows.clear()
    }

    private suspend fun fall() {
        // Compute flows.
        val fromFall = level.flex().flatAssociate(::plusPairs) { (at, l) ->
            sequence {
                // Get primary values.
                val under = at.under()
                val atType = types[at] ?: CellType.none
                val underType = types[under] ?: CellType.none

                // If cannot fall, stop.
                if (!canTravel(atType, underType, Dirs.under) || isFilled(under, underType))
                    return@sequence

                // Get level under and compute the amount that would fall down.
                val target = level[under] ?: 0f
                val flow = minOf(underType.fluidLimit - target, l * underType.flowRate * fps)

                // If no flow, stop.
                if (flow <= 0f)
                    return@sequence

                // Return flow components.
                yield(at to (-flow to Flow.under(-flow)))
                yield(under to (flow to Flow.over(flow)))
            }
        }

        // Update with falling flows.
        level.mergeAll(Float::plusNotZero, fromFall.asSequence().map { (k, v) -> k to v.first })
        flows.mergeAll(Flow::plusNotZero, fromFall.asSequence().map { (k, v) -> k to v.second })

    }

    private suspend fun teleport() {
        // Compute teleport.
        val fromTeleport = level.flex().flatAssociate(::plusPairs) { (at, l) ->
            sequence {
                // Get primary values.
                val under = at.under()
                val atType = types[at] ?: CellType.none
                val underType = types[under] ?: CellType.none

                // If not a teleport cell, stop.
                if (!canTravel(atType, underType, Dirs.under) || !isFilled(under, underType))
                    return@sequence

                // Should teleport, find outlet to teleport to. Start at cell under. If no path found, stop.
                val pathOutlet = findFirstPath(Pair(under, underType), { (f) -> level[f] ?: 0f < l }) { (f, ft) ->
                    sequence {
                        // Check if this is a transitioned node, i.e., if it can teleport.
                        if (!isFilled(f, ft))
                            return@sequence

                        // Get next nodes and directions.
                        for ((n, nd) in f.crossPure() zip f.crossPureDirs()) {
                            // Skip self and higher cells.
                            if (n == f || at.z < n.z)
                                continue

                            // resolve type.
                            val nt = types[n] ?: CellType.none

                            // If connection can be traveled, report it.
                            if (canTravel(ft, nt, nd))
                                yield(Pair(n, nt))
                        }
                    }
                } ?: return@sequence

                // Get last position and type of outlet.
                val (outlet, outletType) = pathOutlet.last()

                // Outlet found, get level at outlet and compute the amount that would teleport.
                val target = level[outlet] ?: 0f
                val flow = if (outlet.z == at.z)
                    minOf(outletType.fluidLimit - target, (l - (l + target) / 2f) * outletType.flowRate * fps)
                else
                    minOf(outletType.fluidLimit - target, l * outletType.flowRate * fps)

                // Check if there is flow, if so, return it, otherwise stop.
                if (flow <= 0f)
                    return@sequence

                // Return level changes.
                yield(at to (-flow to Flow.under(-flow)))
                yield(under to (0f to Flow.over(flow)))
                yield(outlet to (flow to Flow.Zero))

                // For path, add flows.
                for ((a, b) in pathOutlet.zipWithNext()) {
                    // Destruct positions.
                    val (f) = a
                    val (n) = b

                    // Restore direction.
                    val dir = when (n) {
                        f.left() -> Dirs.left
                        f.right() -> Dirs.right
                        f.back() -> Dirs.back
                        f.front() -> Dirs.front
                        f.under() -> Dirs.under
                        f.over() -> Dirs.over
                        else -> Dirs.none
                    }

                    // Add flows.
                    yield(f to (0f to dir.toFlow(-flow)))
                    yield(n to (0f to (-dir).toFlow(flow)))
                }
            }
        }

        // Update with teleport flows.
        level.mergeAll(Float::plusNotZero, fromTeleport.asSequence().map { (k, v) -> k to v.first })
        flows.mergeAll(Flow::plusNotZero, fromTeleport.asSequence().map { (k, v) -> k to v.second })
    }

    private suspend inline fun distribute(dir: Dirs, crossinline extend: (Tri) -> Tri) {
        // Compute distribute.
        val fromDistribute = level.flex().flatAssociate(::plusPairs) { (at, l) ->
            sequence {
                // Get primary values.
                val under = at.under()
                val atType = types[at] ?: CellType.none
                val underType = types[under] ?: CellType.none

                // Check if could travel under, if so, stop.
                if (canTravel(atType, underType, Dirs.under) && !isFilled(under, underType))
                    return@sequence

                // Get next index from extension.
                val next = extend(at)
                val nextType = types[next] ?: CellType.none

                // Check if can travel to next point, otherwise stop.
                if (!canTravel(atType, nextType, dir))
                    return@sequence

                // Get target level or assume zero.
                val target = level[next] ?: 0f

                // If target is greater, target will distribute, stop.
                if (target >= l)
                    return@sequence

                // Get flow as averaging flow.
                val flow = minOf(nextType.fluidLimit - target, (l - (l + target) / 2f) * nextType.flowRate * fps)

                // Return updates.
                yield(at to (-flow to dir.toFlow(-flow)))
                yield(next to (flow to (-dir).toFlow(flow)))
            }
        }

        // Update with distribute flows.
        level.mergeAll(Float::plusNotZero, fromDistribute.asSequence().map { (k, v) -> k to v.first })
        flows.mergeAll(Flow::plusNotZero, fromDistribute.asSequence().map { (k, v) -> k to v.second })
    }

    private fun clearOutside() {
        // Remove all under minimum z level.
        level.keys.removeAll(level.keys.filter { it.z < -20 })
    }


    fun flow(at: Tri) =
        flows[at]

    fun delLevel(at: Tri): Vec {
        val atLeft = at.left()
        val atRight = at.right()
        val atBack = at.back()
        val atFront = at.front()
        val atUnder = at.under()
        val atOver = at.over()
        val levelAtLeft = level[atLeft] ?: 0f
        val levelAtRight = level[atRight] ?: 0f
        val levelAtBack = level[atBack] ?: 0f
        val levelAtFront = level[atFront] ?: 0f
        val levelAtUnder = level[atUnder] ?: 0f
        val levelAtOver = level[atOver] ?: 0f

        return Vec(
            (levelAtRight - levelAtLeft) / 2f,
            (levelAtFront - levelAtBack) / 2f,
            (levelAtOver - levelAtUnder) / 2f
        )
    }

    /**
     * Periodic world update.
     */
    val worldUpdate = repeating(Short.MAX_VALUE, millis, shell::initializedTime) {
        runBlocking {
            resetFlows()
            fall()
            teleport()
            distribute(Dirs.left, Tri::left)
            distribute(Dirs.front, Tri::front)
            distribute(Dirs.right, Tri::right)
            distribute(Dirs.back, Tri::back)
            clearOutside()
        }
    }

    val add = exchange(::doAdd)

    private fun doAdd(coord: Tri) {
        level[coord] = 1f
    }

    var highlight: Tri? = null

    /**
     * Renders all actors.
     */
    fun render(time: Double, delta: Double) {
        types.forEach { (at, _) ->
            if (at !in hidden) {
                val t = types[at] ?: CellType.none
                val display = when {
                    at == highlight -> sx.res.highlight.get().color(Color(1f, 1f, 1f, 0.3f))
                    t == CellTypes.Rock -> sx.res.rock.get()
                    t == CellTypes.Soil -> sx.res.soil.get()
                    else -> sx.res.highlight.get()
                }

                sx.cube(
                    display, at.over() to Tri.Zero, Color.WHITE,
                    Mat.translation(at.x * 4f, at.z * 4f, at.y * 4f).scale(4f, 4f, 4f)
                )
            }
        }
        level.forEach { (at, value) ->
            if (0.01 < value) {
                val display = when {
                    at == highlight -> sx.res.highlight.get().color(Color(1f, 1f, 1f, 0.3f))
                    else -> sx.res.fluid.get().color(Color(1f, 1f, 1f, 0.3f))
                }
                sx.cube(
                    display, at to value, Color.WHITE,
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