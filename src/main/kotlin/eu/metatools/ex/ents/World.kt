package eu.metatools.ex.ents

import eu.metatools.f2d.context.Drawable
import eu.metatools.ex.*
import eu.metatools.ex.math.*
import eu.metatools.f2d.math.*
import eu.metatools.up.Ent
import eu.metatools.up.Shell
import eu.metatools.up.dsl.*
import eu.metatools.up.dt.Lx
import eu.metatools.up.list
import java.util.*

/**
 * The root world entity.
 * @param shell The shell for the [Ent].
 * @param id The ID for the [Ent].
 * @param map The map data.
 */
class World(
    shell: Shell, id: Lx, val ui: Frontend,
    map: Map<Tri, Template>
) : Ent(shell, id), Rendered, TraitDamageable {
    override val extraArgs = mapOf("map" to map)

    val hull = hashMapOf<Int, Regions>()

    val clip = hashMapOf<Int, Regions>()

    val entries = hashMapOf<Int, Regions>()

    val visuals = TreeMap<Tri, MutableList<Drawable<Unit?>>>()

    val flags = hashMapOf<Pair<Tri, String>, Any?>()

    val map by mapObserved({ map }) { changed ->
        changed.removed.forEach { (at, template) ->
            template.unapply(this, at.x, at.y, at.z)
        }
        changed.added.forEach { (at, template) ->
            template.apply(this, at.x, at.y, at.z)
        }
    }

    fun evaluateCollision(clips: Boolean, level: Int, radius: Real, pt: RealPt): Collision {
        val fromCollision = hull[level]?.collision(radius, pt) ?: Collision.NONE
        if (!clips)
            return fromCollision
        val fromClip = clip[level]?.collision(radius, pt) ?: Collision.NONE
        return minOf(fromCollision, fromClip)
    }


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
        if (random.nextDouble() > 0.99 && shell.list<Respack>().count() < 10) {
            val field = flags
                .filterKeys { it.second == "RSP" }
                .filterValues { it == true }
                .toList()
                .let {
                    it[random.nextInt(it.size)].first.first
                }

            constructed(
                Respack(
                    shell, newId(), ui,
                    RealPt(field.x, field.y), 0, 5 + random.nextInt(10)
                )
            )
        }
    }


    /**
     * Set of all movers in the game.
     */
    val movers by set<Mover>()

    override fun render(mat: Mat, time: Double) {
        for ((at, drawables) in visuals)
            for (drawable in drawables)
                ui.submit(
                    drawable, time, mat * Mat
                        .translation(
                            Constants.tileWidth * at.x,
                            Constants.tileHeight * (at.y + at.z),
                            -at.z.toFloat()
                        ).scale(
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
                    RealPt(5f.toReal(), 5f.toReal()), 0, type, owner
                )
            )
        )
    }

    override fun takeDamage(amount: Int) {
        // Nothing to do.
    }
}