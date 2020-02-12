package eu.metatools.ex.ents

import eu.metatools.ex.Frontend
import eu.metatools.ex.ents.Constants.tileHeight
import eu.metatools.ex.ents.Constants.tileWidth
import eu.metatools.ex.ents.hero.Hero
import eu.metatools.ex.ents.hero.Heroes
import eu.metatools.ex.geom.Mesh
import eu.metatools.ex.sec
import eu.metatools.fio.data.Mat
import eu.metatools.fio.data.Tri
import eu.metatools.fio.data.Vec
import eu.metatools.fio.immediate.submit
import eu.metatools.fio.tools.CaptureCube
import eu.metatools.up.Ent
import eu.metatools.up.Shell
import eu.metatools.up.dsl.mapObserved
import eu.metatools.up.dsl.set
import eu.metatools.up.dt.Lx
import eu.metatools.up.list
import java.util.*

fun toZ(y: Number, z: Number) =
    -z.toFloat() + y.toFloat()


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

    val meshes = TreeMap<Tri, Mesh>()

    /**
     * The map. On updates, the [hull] and [bounds] are updated.
     */
    val map by mapObserved({ map }) { changed ->
        changed.removed.forEach { (at, block) ->
            meshes.remove(at)
        }
        changed.added.forEach { (at, block) ->
            meshes[at] = block.mesh(at.x.toFloat(), at.y.toFloat(), at.z.toFloat())
        }
    }

    /**
     * Repeater generating updates in 50ms intervals.
     */
    val worldUpdate = repeating(Short.MAX_VALUE, 50, shell::initializedTime) {
        val seconds = (time.global - shell.initializedTime).sec
        updateMovement(seconds, 50L.sec)
        shell.list<Ticking>().forEach {
            it.update(seconds, 50)
        }

//        return@repeating
//
//        val random = rng()
//
//        val containers = shell.list<Container>()
//        if (random.nextInt(100) < 5 && containers.count() < 10) {
//            val field = map
//                .filter { (k, v) -> v.extras["RSP"] == true && map[k.copy(z = k.z.inc())]?.solid != true }
//                .toList()
//                .takeIf { it.isNotEmpty() }
//                ?.let {
//                    it[random.nextInt(it.size)].first
//                }
//
//            if (field != null) {
//                if (random.nextBoolean())
//                    constructed(
//                        AmmoContainer(
//                            shell, newId(), ui,
//                            QVec(field.x, field.y, field.z.inc()), 5 + random.nextInt(10)
//                        )
//                    )
//                else
//                    constructed(
//                        HealthContainer(
//                            shell, newId(), ui,
//                            QVec(field.x, field.y, field.z.inc()), 5.toQ() + random.nextInt(10)
//                        )
//                    )
//            }
//        }
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
                val z = at.z - 0.5f
                val local = mat
                    .translate(x = tileWidth * x, y = tileHeight * y)
                    .translate(y = tileHeight * z)
                    .translate(z = toZ(y, z))
                    .scale(tileWidth, tileHeight)

                val result = BlockCapture(at, block, true)
                ui.world.submit(CaptureCube, result, time, local)

                ui.world.submit(it, time, local)
            }

            // Draw cap of block.
            block.cap?.let {
                val x = at.x
                val y = at.y
                val z = at.z + 0.5f
                val local = mat
                    .translate(x = tileWidth * x, y = tileHeight * y)
                    .translate(y = tileHeight * z)
                    .translate(z = toZ(y, z) + 1f)
                    .scale(tileWidth, tileHeight)


                // TODO: What here
                val result = BlockCapture(at, block, true)
                ui.world.submit(CaptureCube, result, time, local.scale(sz = 1f / tileHeight))

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
                    Vec(5f, 5f, 0f), Heroes.Pazu, owner
                )
            )
        )
    }
}

data class BlockCapture(val tri: Tri, val block: Block, val cap: Boolean)


fun World.findGround(pos: Vec): Vec? {
    return null
//    val triX = pos.x.roundToInt()
//    val triY = pos.y.roundToInt()
//    val triZ = pos.z.roundToInt()
//
//    for (z in triZ downTo triZ - heightCheckLimit) {
//        // Get mesh under or in.
//        val mesh = meshes[Tri(triX, triY, z)] ?: continue
//
//
//        // Get distance.
//        val (_, distance) = mesh.closest(pos, 0f, Vec.Z, 0f)
//
//        // Return and stop loop.
//        return pos - Vec.Z * distance
//    }
//
//    return null
}