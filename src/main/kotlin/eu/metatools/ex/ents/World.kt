package eu.metatools.ex.ents

import eu.metatools.f2d.context.Drawable
import eu.metatools.ex.*
import eu.metatools.ex.ents.TileShape.*
import eu.metatools.ex.math.*
import eu.metatools.f2d.context.under
import eu.metatools.f2d.math.*
import eu.metatools.f2d.tools.Static
import eu.metatools.up.Ent
import eu.metatools.up.Shell
import eu.metatools.up.dsl.*
import eu.metatools.up.dt.Lx
import eu.metatools.up.list

enum class TileShape {
    Passable,
    Solid,
    WedgeTopLeft,
    WedgeTopRight,
    WedgeBottomRight,
    WedgeBottomLeft,
    Circle
}

/**
 * A tile kind.
 */
interface TileKind {
    /**
     * The visual to draw.
     */
    val visual: Drawable<Unit?>

    /**
     * Shape of the tile
     */
    val shape: TileShape
}

/**
 * Some instances of tiles.
 */
enum class Tiles : TileKind {
    Ground {
        override val visual by lazy {
            Resources.terrain[Static("tile109")]
        }

        override val shape = Passable

    },
    Wall {
        override val visual by lazy {
            Resources.terrain[Static("tile462")]
        }

        override val shape = Solid

    },
    Cover {
        override val visual by lazy {
            Resources.terrain[Static("tile614")]
        }

        override val shape = Passable

    },
    Edge {
        override val visual by lazy {
            Ground.visual under
                    Resources.terrain[Static("tile231")]
        }

        override val shape = WedgeBottomLeft
    }
}

/**
 * The root world entity.
 * @param shell The shell for the [Ent].
 * @param id The ID for the [Ent].
 * @param map The map data.
 */
class World(
    shell: Shell, id: Lx, val ui: Frontend,
    map: Map<Tri, TileKind>
) : Ent(shell, id), Rendered, TraitDamageable {
    override val extraArgs = mapOf("map" to map)
    /**
     * Collision composer.
     */
    private val collisions = hashMapOf<Int, Hull>()

    fun evaluateCollision(level: Int, radius: Real, pt: RealPt) =
        collisions[level]?.evaluate(radius, pt) ?: Collision.NONE


    var res by prop { 0 }

    /**
     * Repeater generating updates in 50ms intervals.
     */
    val worldUpdate = repeating(Short.MAX_VALUE, 50, shell::initializedTime) {
        shell.list<Ticking>().forEach {
            it.update((time.global - shell.initializedTime).sec, 50)
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
                Triple(rx, ry, tiles[Tri(rx, ry, 0)])
            }.first { (_, _, v) -> v?.shape == Passable }

            constructed(
                Respack(
                    shell, newId(), ui,
                    RealPt(sx.toReal(), sy.toReal()), 10
                )
            )

        }
    }

    private fun evalShape(x: Int, y: Int, shape: TileShape) = when (shape) {
        Passable -> null
        Solid -> polyRect(RealPt.from(x - 0.5, y - 0.5), RealPt.from(x + 0.5, y + 0.5))
        WedgeTopLeft -> polyWedge(RealPt.from(x - 0.5, y - 0.5), RealPt.from(x + 0.5, y + 0.5), Corner.TopLeft)
        WedgeTopRight -> polyWedge(RealPt.from(x - 0.5, y - 0.5), RealPt.from(x + 0.5, y + 0.5), Corner.TopLeft)
        WedgeBottomRight -> polyWedge(RealPt.from(x - 0.5, y - 0.5), RealPt.from(x + 0.5, y + 0.5), Corner.BottomRight)
        WedgeBottomLeft -> polyWedge(RealPt.from(x - 0.5, y - 0.5), RealPt.from(x + 0.5, y + 0.5), Corner.BottomLeft)
        Circle -> polyCircle(RealPt.from(x, y), 0.5.toReal())
    }

    /**
     * The map from world location to tile kind. Changes update the SDF composer.
     */
    val tiles by mapObserved<Tri, TileKind>({ map }) {
        // Remove impassable tiles.
        for ((k, v) in it.removed)
            evalShape(k.x, k.y, v.shape)?.let { poly ->
                collisions[k.z]?.union?.remove(poly)
            }

        // Add new impassable tiles.
        for ((k, v) in it.added)
            evalShape(k.x, k.y, v.shape)?.let { poly ->
                collisions.getOrPut(k.z, ::Hull).union.add(poly)
            }
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
                    .translation(
                        Constants.tileWidth * k.x,
                        Constants.tileHeight * k.y + Constants.tileDepth * k.z,
                        -k.z.toFloat()
                    )
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