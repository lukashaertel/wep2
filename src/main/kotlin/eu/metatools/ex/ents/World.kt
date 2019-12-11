package eu.metatools.ex.ents

import eu.metatools.f2d.context.Drawable
import eu.metatools.ex.*
import eu.metatools.ex.math.SDFComposer
import eu.metatools.f2d.math.Mat
import eu.metatools.f2d.math.Pt
import eu.metatools.f2d.math.Cell
import eu.metatools.f2d.tools.Static
import eu.metatools.up.Ent
import eu.metatools.up.Shell
import eu.metatools.up.dsl.*
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
class World(shell: Shell, id: Lx, map: Map<Cell, TileKind>) : Ent(shell, id), Rendered, TraitDamageable {
    override val extraArgs = mapOf("map" to map)

    /**
     * The composer building the SDF.
     */
    private val clipping = SDFComposer()

    /**
     * Pre-computed SDFs per radius.
     */
    private val sdfs = mutableMapOf<Float, (Pt) -> Float>()

    /**
     * Gets the SDF for the given radius.
     */
    fun sdf(radius: Float) =
        sdfs.getOrPut(radius) {
            clipping.sdf(radius)
        }

    /**
     * Repeater generating updates in 40ms intervals.
     */
    val worldUpdate = repeating(40, shell::initializedTime) {
        shell.list<Ticking>().forEach {
            require(it !is Ent || it.driver.isConnected)
            it.update((time.global - shell.initializedTime).sec, 40)
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
            frontend.continuous.submit(
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
        // Get RNG to determine what mover to spawn.
        val rng = rng()
        val type = if (rng.nextBoolean()) Movers.Small else Movers.Large

        // Add mover to set of movers.
        movers.add(
            constructed(
                Mover(
                    shell,
                    newId(),
                    Pt(5f, 5f),
                    type,
                    owner
                )
            )
        )
    }

    override fun takeDamage(amount: Int) {
        // Nothing to do.
    }
}