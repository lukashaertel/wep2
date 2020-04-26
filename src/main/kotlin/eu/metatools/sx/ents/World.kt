package eu.metatools.sx.ents

import com.badlogic.gdx.graphics.Color
import eu.metatools.fio.data.Mat
import eu.metatools.fio.data.Tri
import eu.metatools.sx.SX
import eu.metatools.sx.data.Volume
import eu.metatools.sx.data.merge
import eu.metatools.sx.data.volume
import eu.metatools.sx.process.ProcessAt
import eu.metatools.up.Ent
import eu.metatools.up.Shell
import eu.metatools.up.dsl.set
import eu.metatools.up.dt.Lx

//
//class Cell(
//    shell: Shell, id: Lx, val sx: SX,
//    val behavior: Behavior,
//    initState: Any
//) : Ent(shell, id) {
//    val world get() = sx.root
//
//    override val extraArgs = mapOf(
//        "initState" to initState
//    )
//
//    var state by propObserved({ initState }, initState) {
//        if (it.isChange())
//            world.stateChanged(this, it.from, it.to)
//    }
//
//    fun dependencies(): NavigableMap<Cell, Any> =
//        world.cellIn(this).associateWithTo(TreeMap()) { it.state }
//}
//
//interface Behavior {
//    // TODO: Locality, optimized queries.
//    /**
//     * Gets all cells this definition depends on.
//     */
//    fun depend(self: Cell, world: World): Sequence<Cell> =
//        world.cells
//            .asSequence()
//            .filter { actor ->
//                dependsOn(self, actor, world)
//            }
//
//    /**
//     * True if the containing depends on the given cell.
//     */
//    fun dependsOn(self: Cell, cell: Cell, world: World): Boolean
//
//    /**
//     * Reacts to a change.
//     */
//    fun react(self: Cell, to: MapChange<Cell, Any>): Any
//}


//    private val cellsOut = TreeMap<Cell, SortedSet<Cell>>()
//
//    private val cellsIn = TreeMap<Cell, SortedSet<Cell>>()
//
//    fun cellOut(cell: Cell) =
//        cellsOut[cell].orEmpty()
//
//    fun cellIn(cell: Cell) =
//        cellsIn[cell].orEmpty()
//
//    val cells: NavigableSet<Cell> by setObserved { (added, removed) ->
//        // Remove connection of removed cells.
//        for (cell in removed) {
//            // Remove outward tracking.
//            cellsOut.remove(cell)?.forEach {
//                // Remove cell from cells depending on it.
//                cellsIn[it]?.remove(cell)
//
//                // Collect removal on dependency.
//                collectChange(it, MapChange(sortedMapOf(), sortedMapOf(cell to cell.state)))
//            }
//
//            // Remove inward tracking.
//            cellsIn.remove(cell)?.forEach {
//                // Remove from dependency's output.
//                cellsOut[it]?.remove(cell)
//            }
//        }
//
//        // Update existing cells, excluding now added cells.
//        for (existing in cells) if (existing !in added)
//            for (cell in added)
//                if (existing.behavior.dependsOn(existing, cell, this)) {
//                    // Add connection to added cell if depending.
//                    cellsIn.getOrPut(existing, ::TreeSet).add(cell)
//                    cellsOut.getOrPut(cell, ::TreeSet).add(existing)
//
//                    // Tag for update, added cell is collected with state.
//                    collectChange(existing, MapChange(sortedMapOf(cell to cell.state), sortedMapOf()))
//                }
//
//        // Add dependencies of added cells.
//        for (cell in added) {
//            // Get dependency of new cell.
//            val depends = cell.behavior.depend(cell, this)
//
//            // Add all to cell in.
//            cellsIn.getOrPut(cell, ::TreeSet).addAll(depends)
//
//            // Add outward tracking.
//            for (other in depends)
//                cellsOut.getOrPut(other, ::TreeSet).add(cell)
//
//            // Collect change by associating added dependents with their state.
//            collectChange(cell, MapChange(depends.associateWithTo(TreeMap()) { it.state }, sortedMapOf()))
//        }
//    }
//
//    private val collected by map<Cell, MapChange<Cell, Any>>()
//
//    private fun collectChange(cell: Cell, change: MapChange<Cell, Any>) {
//        collected.compute(cell) { _, existing ->
//            existing?.merge(change) ?: change
//        }
//    }


//        val stack = collected.toList()
//        collected.clear()
//
//        val updates = TreeMap<Cell, Any>()
//        stack.forEach { (cell, change) ->
//            updates[cell] = cell.behavior.react(cell, change)
//        }
//
//        for ((cell, state) in updates)
//            cell.state = state

//for (actor in actors)
// actor.render(time, delta)

data class Fluid(
    val mass: Int,
    val left: Int = 0,
    val right: Int = 0,
    val front: Int = 0,
    val back: Int = 0,
    val above: Int = 0,
    val below: Int = 0
) {
    companion object {
        val ZERO = Fluid(0)
    }

    operator fun unaryMinus() =
        Fluid(-mass, -left, -right, -front, -back, -above, -below)

    operator fun plus(other: Fluid) =
        Fluid(
            mass + other.mass,
            left + other.left,
            right + other.right,
            front + other.front,
            back + other.back,
            above + other.above,
            below + other.below
        )

    fun hasHorizontalFlow() =
        left != 0 || right != 0 || front != 0 || back != 0

    fun hasVerticalFlow() =
        above != 0 || below != 0

    val cardinalDirection by lazy {
        Tri(right - left, front - back, above - below)
    }

    val inFlow by lazy {
        left.coerceAtLeast(0) +
                right.coerceAtLeast(0) +
                front.coerceAtLeast(0) +
                back.coerceAtLeast(0) +
                above.coerceAtLeast(0) +
                below.coerceAtLeast(0)
    }

    val outFlow by lazy {
        -left.coerceAtMost(0) -
                right.coerceAtMost(0) -
                front.coerceAtMost(0) -
                back.coerceAtMost(0) -
                above.coerceAtMost(0) -
                below.coerceAtMost(0)
    }
}

// TODO: Flow direction of fluids.

class World(
    shell: Shell, id: Lx, val sx: SX
) : Ent(shell, id) {
    val players by set<Player>()

    val solid by volume<Int>()

    val fluid by volume<Fluid>()

    val viscosity by volume<Int>()

    companion object {
        const val maxMass = 1024
    }

    val proc = object : ProcessAt<Fluid, Fluid>() {
        override fun merge(first: Fluid, second: Fluid) =
            first + second

        override fun computeAt(volume: Volume<Fluid>, x: Int, y: Int, z: Int, value: Fluid) = sequence {
            // https://is.muni.cz/th/mfkg2/master-thesis.pdf

            fun viscosity(x: Int, y: Int, z: Int) =
                (1024 - (viscosity[x, y, z] ?: 0))

            infix fun Int.tm(other: Int) = this * other / 1024

            // Emit dynamics-reset vector.
            yield(Tri(x, y, z) to -value.copy(mass = 0))

            fun s(x: Int, y: Int, z: Int) =
                solid[x, y, z] ?: 0

            fun f(x: Int, y: Int, z: Int) =
                volume[x, y, z]?.mass ?: 0

            fun curr(x: Int, y: Int, z: Int) =
                s(x, y, z) + f(x, y, z)

            val currSelf = s(x, y, z) + value.mass

            // Calculate and yield compression.
            val compressOut = maxOf(0, currSelf - maxMass) tm viscosity(x, y, z.inc())
            if (compressOut != 0) {
                // Water compressing upward, change masses and update flows.
                yield(Tri(x, y, z) to Fluid(-compressOut, above = -compressOut))
                yield(Tri(x, y, z.inc()) to Fluid(compressOut, below = compressOut))
                return@sequence
            }

            fun free(x: Int, y: Int, z: Int) =
                maxOf(0, maxMass - curr(x, y, z))

            // Calculate and yield falling.
            val fallDown = value.mass.coerceIn(0, free(x, y, z.dec())) tm viscosity(x, y, z.dec())
            if (fallDown != 0) {
                // Water falling down, change masses and update flows.
                yield(Tri(x, y, z) to Fluid(-fallDown, below = -fallDown))
                yield(Tri(x, y, z.dec()) to Fluid(fallDown, above = fallDown))
                return@sequence
            }

            fun diff(currA: Int, currB: Int, fA: Int) =
                (currA - currB).coerceIn(0, maxOf(0, fA)) / 5

            // Calculate differentials, apply target cell viscosity.
            val toLeft = diff(currSelf, curr(x.dec(), y, z), value.mass) tm viscosity(x.dec(), y, z)
            val toRight = diff(currSelf, curr(x.inc(), y, z), value.mass) tm viscosity(x.inc(), y, z)
            val toFront = diff(currSelf, curr(x, y.inc(), z), value.mass) tm viscosity(x, y.inc(), z)
            val toBack = diff(currSelf, curr(x, y.dec(), z), value.mass) tm viscosity(x, y.dec(), z)

            if (toLeft != 0) {
                // Water flowing to left, change masses and update flows.
                yield(Tri(x, y, z) to Fluid(-toLeft, left = -toLeft))
                yield(Tri(x.dec(), y, z) to Fluid(toLeft, right = toLeft))
            }

            if (toRight != 0) {
                // Water flowing to right, change masses and update flows.
                yield(Tri(x, y, z) to Fluid(-toRight, right = -toRight))
                yield(Tri(x.inc(), y, z) to Fluid(toRight, left = toRight))
            }

            if (toFront != 0) {
                // Water flowing to front, change masses and update flows.
                yield(Tri(x, y, z) to Fluid(-toFront, front = -toFront))
                yield(Tri(x, y.inc(), z) to Fluid(toFront, back = toFront))
            }

            if (toBack != 0) {
                // Water flowing to back, change masses and update flows.
                yield(Tri(x, y, z) to Fluid(-toBack, back = -toBack))
                yield(Tri(x, y.dec(), z) to Fluid(toBack, front = toBack))
            }

        }
    }

    /**
     * Periodic world update.
     */
    val worldUpdate = repeating(Short.MAX_VALUE, 50, shell::initializedTime) {
        fluid.merge(proc.compute(fluid)) { ex, delta ->
            ((ex ?: Fluid.ZERO) + delta).takeIf { it.mass != 0 }
        }

        // fluid.getAll().sumBy { it.second.mass }.let(::println)

        // Remove outside of bounds.
        val oob = fluid[Int.MIN_VALUE..Int.MAX_VALUE, Int.MIN_VALUE..Int.MAX_VALUE, Int.MIN_VALUE..-20].associate {
            it.first to null
        }
        fluid.assign(oob)
    }

    val add = exchange(::doAdd)

    private fun doAdd(coord: Tri) {
        val target = fluid[coord.x, coord.y, coord.z] ?: Fluid.ZERO
        fluid[coord.x, coord.y, coord.z] = target.copy(mass = maxMass)
    }

    /**
     * Renders all actors.
     */
    fun render(time: Double, delta: Double) {
        solid.getAll().forEach { (at, value) ->
            if (value > 0)
                sx.cube(
                    at.copy(z = at.z.inc()) to Tri.Zero, Color.WHITE,
                    Mat.translation(at.x * 4f, at.z * 4f, at.y * 4f).scale(4f, 4f, 4f)
                )
        }
        fluid.getAll().forEach { (at, value) ->
            if (value.mass > 0) {
                val scale = value.mass.toFloat() / maxMass
                if (value.hasVerticalFlow() && !value.hasHorizontalFlow())
                    sx.cube(
                        at to value.cardinalDirection,
                        Color(0f, 1f, 1f, 0.4f),
                        Mat.translation(at.x * 4f, at.z * 4f, at.y * 4f)
                            .scale(4f, 4f, 4f)
                            .translate(0f, -0.5f, 0f)
                            .scale(sx = scale, sz = scale)
                            .translate(0f, 0.5f, 0f)
                    )
                else
                    sx.cube(
                        at to value.cardinalDirection,
                        Color(0f, 1f, 1f, 0.4f),
                        Mat.translation(at.x * 4f, at.z * 4f, at.y * 4f)
                            .scale(4f, 4f, 4f)
                            .translate(0f, -0.5f, 0f)
                            .scale(sy = scale)
                            .translate(0f, 0.5f, 0f)
                    )
            }
        }
    }
}