package eu.metatools.ex.ents

import eu.metatools.f2d.context.Drawable
import eu.metatools.ex.*
import eu.metatools.ex.math.SDFComposer
import eu.metatools.f2d.context.UI
import eu.metatools.f2d.math.*
import eu.metatools.f2d.tools.Cube
import eu.metatools.f2d.tools.Static
import eu.metatools.up.Ent
import eu.metatools.up.Shell
import eu.metatools.up.dsl.*
import eu.metatools.up.dt.Box
import eu.metatools.up.dt.Lx
import eu.metatools.up.list

/**
 * A tile kind.
 */
interface TileKind {
    /**
     * The visual to draw.
     */
    val visual: Drawable<Unit?>

    /**
     * True if passable.
     */
    val passable: Boolean
}

/**
 * Some instances of tiles.
 */
enum class Tiles : TileKind {
    Ground {
        override val visual by lazy {
            Resources.terrain[Static("tile390")]
        }

        override val passable: Boolean
            get() = true

    },
    Wall {
        override val visual by lazy {
            Resources.terrain[Static("tile702")]
        }
        override val passable: Boolean
            get() = false

    }
}

/**
 * The root world entity.
 * @param shell The shell for the [Ent].
 * @param id The ID for the [Ent].
 * @param map The map data.
 */
class World(
    shell: Shell, id: Lx, val ui: UI,
    map: Map<Cell, TileKind>
) : Ent(shell, id), Rendered, TraitDamageable {
    override val extraArgs = mapOf("map" to map)

    /**
     * The composer building the SDF.
     */
    private val clipping = SDFComposer()

    /**
     * Pre-computed SDFs per radius.
     */
    private val sdfs = mutableMapOf<Real, (RealPt) -> Real>()

    /**
     * Gets the SDF for the given radius.
     */
    fun sdf(radius: Real) =
        sdfs.getOrPut(radius) {
            clipping.sdf(radius)
        }

    var res by prop { 0 }

    /**
     * Repeater generating updates in 40ms intervals.
     */
    val worldUpdate = repeating(Short.MAX_VALUE, 40, shell::initializedTime) {
        shell.list<Ticking>().forEach {
            it.update((time.global - shell.initializedTime).sec, 40)
        }

        if (res < 100)
            res += 1

        val random = rng()
        if (random.nextDouble() > 0.99 && tiles.isNotEmpty() && shell.list<Respack>().count() < 10) {
            val xmin = tiles.keys.map { it.x }.min()!!
            val xmax = tiles.keys.map { it.x }.max()!!
            val ymin = tiles.keys.map { it.y }.min()!!
            val ymax = tiles.keys.map { it.y }.max()!!

            val (sx, sy) = generateSequence {
                val rx = random.nextInt(xmax + 1 - xmin) + xmin
                val ry = random.nextInt(ymax + 1 - ymin) + ymin
                Triple(rx, ry, tiles[Cell(rx, ry)])
            }.first { (_, _, v) -> v?.passable == true }

            constructed(Respack(shell, newId(), RealPt(sx.toReal(), sy.toReal()), 10))

        }
    }

    /**
     * The map from world location to tile kind. Changes update the SDF composer.
     */
    val tiles by mapObserved<Cell, TileKind>({ map }) {
        // Remove impassable tiles.
        for ((k, v) in it.removed)
            if (!v.passable)
                clipping.remove(k.x, k.y)

        // Add new impassable tiles.
        for ((k, v) in it.added)
            if (!v.passable)
                clipping.add(k.x, k.y)

        // Reset memorization cache.
        sdfs.clear()
    }

    /**
     * Set of all movers in the game.
     */
    val movers by set<Mover>()

    override fun render(time: Double) {
        // Render all times.
        for ((k, v) in tiles)
            ui.submit(
                v.visual, time, Mat
                    .translation(Constants.tileWidth * k.x, Constants.tileHeight * k.y)
                    .scale(
                        Constants.tileWidth,
                        Constants.tileHeight
                    )
            )
    }

    /**
     * Creates a mover at a predefine location for the owner.
     */
    val createMover = exchange(::onCreateMover)

    private fun onCreateMover(owner: Short) {
        if (movers.any { it.owner == owner })
            return

        // Get RNG to determine what mover to spawn.
        val rng = rng()
        val type = if (rng.nextBoolean()) Movers.Small else Movers.Large

        // Add mover to set of movers.
        movers.add(
            constructed(
                Mover(
                    shell, newId(), ui,
                    RealPt(5f.toReal(), 5f.toReal()), type, owner
                )
            )
        )
    }

    override fun takeDamage(amount: Int) {
        // Nothing to do.
    }
}