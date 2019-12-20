package eu.metatools.ex.ents

import eu.metatools.f2d.context.Drawable
import eu.metatools.ex.*
import eu.metatools.ex.data.Material
import eu.metatools.ex.data.PolyTemplates
import eu.metatools.ex.math.*
import eu.metatools.f2d.context.over
import eu.metatools.f2d.context.shift
import eu.metatools.f2d.math.*
import eu.metatools.f2d.tools.Static
import eu.metatools.up.Ent
import eu.metatools.up.Shell
import eu.metatools.up.dsl.*
import eu.metatools.up.dt.Lx
import eu.metatools.up.list
import java.util.*


interface Tile {
    val visual: Drawable<Unit?>? get() = null
    val material: Material? get() = null
    val collision: List<Poly>? get() = null
    val blockers: List<Poly>? get() = null
    val exits: List<Pair<Poly, Int>>? get() = null
    val extras: Map<String, Any>? get() = null
}

enum class StandardTile : Tile {
    Floor {
        override val visual by lazy {
            Resources.terrain[Static("tile171")]
        }
        override val material = Material.Stone
        override val extras = mapOf("RSP" to true)
    },
    Tile {
        override val visual by lazy {
            Resources.terrain[Static("tile614")]
        }
        override val material = Material.Stone
        override val extras = mapOf("RSP" to true)
    },
    Wall {
        override val visual by lazy {
            Resources.terrain[Static("tile492")]
        }
        override val collision =
            PolyTemplates.Block.poly
        override val blockers: List<Poly>?
            get() = collision
        override val material = Material.Stone
    },
    StairsFst {
        override val visual by lazy {
            Resources.terrain[Static("tile618")]
        }
        override val material = Material.Stone
        override val collision =
            PolyTemplates.HalfRampLeftFst.poly + PolyTemplates.HalfRampCapLeftFst.poly
        override val blockers: List<Poly>?
            get() = collision
        override val exits =
            PolyTemplates.TrapezoidRight.poly.map { it to 1 } +
                    PolyTemplates.TrapezoidLeft.poly.map { it.move(RealPt(1f, 0f)) to 0 }
    },
    StairsSnd {
        override val visual by lazy {
            Resources.terrain[Static("tile617")]
        }
        override val material = Material.Stone
        override val collision =
            PolyTemplates.HalfRampLeftSnd.poly + PolyTemplates.HalfRampCapLeftSnd.poly
        override val blockers: List<Poly>?
            get() = collision
    },
    StairsTopFst {
        override val material = Material.Stone
        override val visual by lazy {
            Resources.terrain[Static("tile586")]
        }
        override val blockers =
            PolyTemplates.HalfRampLeftFst.poly + PolyTemplates.HalfRampCapLeftFst.poly
    },
    StairsTopSnd {
        override val material = Material.Stone
        override val visual by lazy {
            Resources.terrain[Static("tile585")]
        }
        override val blockers =
            PolyTemplates.HalfRampLeftSnd.poly + PolyTemplates.HalfRampCapLeftSnd.poly
    },
    Blocking {
        override val blockers =
            PolyTemplates.Block.poly
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
    map: Map<Tri, Tile>
) : Ent(shell, id), Rendered, TraitDamageable {
    override val extraArgs = mapOf("map" to map)
    /**
     * Collision composer.
     */
    val collisions = hashMapOf<Pair<Boolean, Int>, Regions>()

    val entries = hashMapOf<Int, Regions>()

    fun evaluateCollision(flying: Boolean, level: Int, radius: Real, pt: RealPt) =
        collisions[flying to level]?.collision(radius, pt) ?: Collision.NONE


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
            }.first { (_, _, v) -> v?.extras?.get("RSP") == true }

            constructed(
                Respack(
                    shell, newId(), ui,
                    RealPt(sx.toReal(), sy.toReal()), 0, 10
                )
            )

        }
    }

    /**
     * The map from world location to tile kind. Changes update the SDF composer.
     */
    val tiles by mapObserved<Tri, Tile>({ map }) {
        for ((k, v) in it.removed) {
            val offset = RealPt(k.x, k.y)
            v.collision?.let { polys ->
                for (poly in polys)
                    collisions[true to k.z]?.union?.remove(poly.move(offset))
            }
            v.blockers?.let { polys ->
                for (poly in polys)
                    collisions[false to k.z]?.union?.remove(poly.move(offset))
            }
            v.exits?.let { shifts ->
                for ((poly, displacement) in shifts)
                    entries[k.z + displacement]?.union?.remove(poly.move(offset))
            }
        }


        for ((k, v) in it.added) {
            val offset = RealPt(k.x, k.y)
            v.collision?.let { polys ->
                for (poly in polys) {
                    collisions.getOrPut(true to k.z, ::Regions).union.add(poly.move(offset))
                }
            }
            v.blockers?.let { polys ->
                for (poly in polys) {
                    collisions.getOrPut(false to k.z, ::Regions).union.add(poly.move(offset))
                }
            }
            v.exits?.let { shifts ->
                for ((poly, displacement) in shifts)
                    entries.getOrPut(k.z + displacement, ::Regions).union.add(poly.move(offset))
            }
        }
    }

    /**
     * Set of all movers in the game.
     */
    val movers by set<Mover>()

    override fun render(mat: Mat, time: Double) {
        // Render all times.
        for ((k, v) in tiles)
            v.visual?.let {
                ui.submit(
                    it, time, mat * Mat
                        .translation(
                            Constants.tileWidth * k.x,
                            Constants.tileHeight * (k.y + k.z),
                            -k.z.toFloat()
                        )
                        .scale(
                            Constants.tileWidth,
                            Constants.tileHeight
                        )
                )
            }

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
                    RealPt(5f.toReal(), 5f.toReal()), 0, type, owner
                )
            )
        )
    }

    override fun takeDamage(amount: Int) {
        // Nothing to do.
    }
}