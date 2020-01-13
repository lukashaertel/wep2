package eu.metatools.ex.ents

import eu.metatools.ex.Frontend
import eu.metatools.ex.ents.Constants.tileHeight
import eu.metatools.ex.ents.Constants.tileWidth
import eu.metatools.ex.sec
import eu.metatools.f2d.data.*
import eu.metatools.f2d.immediate.submit
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

    val hull = Hull()

    val bounds = Hull()

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
        if (random.nextDouble() > 0.98 && shell.list<Respack>().count() < 10) {
            val field = map
                .filter { (k, v) -> v.extras["RSP"] == true && map[k.copy(z = k.z.inc())]?.solid != true }
                .toList()
                .takeIf { it.isNotEmpty() }
                ?.let {
                    it[random.nextInt(it.size)].first
                }

            if (field != null)
                constructed(
                    Respack(
                        shell, newId(), ui,
                        QPt(field.x, field.y), field.z.inc().toQ(), 5 + random.nextInt(10)
                    )
                )
        }
    }


    /**
     * Set of all movers in the game.
     */
    val movers by set<Mover>()

    override fun render(mat: Mat, time: Double) {
        for ((at, block) in map) {
            // Draw body of block.
            block.body?.let {
                val x = at.x
                val y = at.y
                val z = at.z
                ui.world.submit(
                    it, time, mat
                        .translate(x = tileWidth * x, y = tileHeight * y)
                        .translate(y = tileHeight * z)
                        .translate(z = toZ(z))
                        .scale(tileWidth, tileHeight)
                )
            }

            // Draw cap of block.
            block.cap?.let {
                val x = at.x
                val y = at.y
                val z = at.z + 1
                ui.world.submit(
                    it, time, mat
                        .translate(x = tileWidth * x, y = tileHeight * y)
                        .translate(y = tileHeight * z)
                        .translate(z = toZ(z))
                        .scale(tileWidth, tileHeight)
                )
            }
        }
    }

    /**
     * Creates a mover at a predefine location for the owner.
     */
    val createMover = exchange(::onCreateMover)

    private fun onCreateMover(owner: Short) {
        if (movers.any { it.owner == owner })
            return

        // Add mover to set of movers.
        movers.add(
            constructed(
                Mover(
                    shell, newId(), ui,
                    QPt(5f.toQ(), 5f.toQ()), Q.ZERO, Movers.Pazu, owner
                )
            )
        )
    }
}