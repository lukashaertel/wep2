package eu.metatools.ex.ents

import eu.metatools.ex.Frontend
import eu.metatools.ex.ents.Constants.tileHeight
import eu.metatools.ex.ents.Constants.tileWidth
import eu.metatools.ex.ents.hero.Hero
import eu.metatools.ex.ents.hero.Heroes
import eu.metatools.ex.ents.items.AmmoContainer
import eu.metatools.ex.ents.items.Container
import eu.metatools.ex.ents.items.HealthContainer
import eu.metatools.ex.sec
import eu.metatools.f2d.data.*
import eu.metatools.f2d.immediate.submit
import eu.metatools.f2d.tools.CaptureCube
import eu.metatools.up.Ent
import eu.metatools.up.Shell
import eu.metatools.up.dsl.mapObserved
import eu.metatools.up.dsl.set
import eu.metatools.up.dt.Lx
import eu.metatools.up.list
import kotlin.math.ceil

fun toZ(level: Number) =
    -ceil(level.toFloat())

/**
 * The root world entity.
 * @param shell The shell for the [Ent].
 * @param id The ID for the [Ent].
 * @param map The map data.
 */
class World(
    shell: Shell, id: Lx, val ui: Frontend, map: Map<Tri, Block>
) : Ent(shell, id), Rendered {
    override val extraArgs = mapOf("map" to map)
    /**
     * World geometry hull.
     */
    val hull = Hull()

    /**
     * World geometry walkable hull.
     */
    val bounds = Hull()

    /**
     * The map. On updates, the [hull] and [bounds] are updated.
     */
    val map by mapObserved({ map }) { changed ->
        changed.removed.forEach { (at, block) ->
            if (block.solid) hull.remove(at.z, at.x, at.y)
            if (block.walkable) bounds.remove(at.z + 1, at.x, at.y)
        }
        changed.added.forEach { (at, block) ->
            if (block.solid) hull.add(at.z, at.x, at.y)
            if (block.walkable) bounds.add(at.z + 1, at.x, at.y)
        }
    }

    /**
     * Repeater generating updates in 50ms intervals.
     */
    val worldUpdate = repeating(Short.MAX_VALUE, 50, shell::initializedTime) {
        val seconds = (time.global - shell.initializedTime).sec
        updateMovement(seconds)
        shell.list<Ticking>().forEach {
            it.update(seconds, 50)
        }

        val random = rng()
        val containers = shell.list<Container>()
        if (random.nextInt(100) < 5 && containers.count() < 10) {
            val field = map
                .filter { (k, v) ->
                    v.extras["RSP"] == true
                            && map[k.copy(z = k.z.inc())]?.solid != true
                            && containers.none { it.xy == QPt(k.x, k.y) && it.z == Q(k.z) }
                }

                .toList()
                .takeIf { it.isNotEmpty() }
                ?.let {
                    it[random.nextInt(it.size)].first
                }

            if (field != null) {
                if (random.nextBoolean())
                    constructed(
                        AmmoContainer(
                            shell, newId(), ui,
                            QPt(field.x, field.y), field.z.inc().toQ(), 5 + random.nextInt(10)
                        )
                    )
                else
                    constructed(
                        HealthContainer(
                            shell, newId(), ui,
                            QPt(field.x, field.y), field.z.inc().toQ(), 5.toQ() + random.nextInt(10)
                        )
                    )
            }
        }
    }


    /**
     * Set of all heroes in the game.
     */
    val heroes by set<Hero>()

    override fun render(mat: Mat, time: Double) {
        for ((at, block) in map) {
            // Draw body of block.
            block.body?.let {
                val x = at.x
                val y = at.y
                val z = at.z
                val local = mat
                    .translate(x = tileWidth * x, y = tileHeight * y)
                    .translate(y = tileHeight * z)
                    .translate(z = toZ(z))
                    .scale(tileWidth, tileHeight)

                if (block.solid) {
                    val result = BlockCapture(at, block, true)
                    ui.world.submit(CaptureCube, result, time, local)
                }
                ui.world.submit(it, time, local)
            }

            // Draw cap of block.
            block.cap?.let {
                val x = at.x
                val y = at.y
                val z = at.z + 1
                val local = mat
                    .translate(x = tileWidth * x, y = tileHeight * y)
                    .translate(y = tileHeight * z)
                    .translate(z = toZ(z))
                    .scale(tileWidth, tileHeight)


                if (block.walkable) {
                    val result = BlockCapture(at, block, true)
                    ui.world.submit(CaptureCube, result, time, local.scale(sz = 1f / tileHeight))
                }
                ui.world.submit(it, time, local)
            }
        }
    }

    /**
     * Creates a hero at a predefine location for the owner.
     */
    val createHero = exchange(::onCreateHero)

    private fun onCreateHero(owner: Short) {
        // Player already has hero, return.
        if (heroes.any { it.owner == owner })
            return

        // Construct and add hero.
        heroes.add(
            constructed(
                Hero(
                    shell, newId(), ui,
                    QPt(5f.toQ(), 5f.toQ()), 0, Heroes.Pazu, owner
                )
            )
        )
    }
}

data class BlockCapture(val tri: Tri, val block: Block, val cap: Boolean)