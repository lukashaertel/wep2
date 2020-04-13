package eu.metatools.sx.ents

import eu.metatools.sx.SX
import eu.metatools.up.Ent
import eu.metatools.up.Shell
import eu.metatools.up.dsl.map
import eu.metatools.up.dsl.set
import eu.metatools.up.dsl.setObserved
import eu.metatools.up.dt.Lx
import eu.metatools.up.dt.lx
import java.util.*
import kotlin.collections.ArrayList

class World(
    shell: Shell,
    id: Lx,
    sx: SX
) : Ent(shell, lx) {
    val players by set<Player>()

    /**
     * Forward dependencies of the federated graph.
     */
    private val dependForward by map<Actor<*, *>, Set<Actor<*, *>>>()

    /**
     * Backward dependencies of the federated graph.
     */
    private val dependBackward by map<Actor<*, *>, Set<Actor<*, *>>>()

    /**
     * Dependency graph.
     */
    private val depend = FederatedGraph(::dependForward, ::dependBackward)

    /**
     * Actor set, change to this causes reevaluation of some dependency queries.
     */
    val actors: NavigableSet<Actor<*, *>> by setObserved { (added, removed) ->
        // First part, of all actors that depend on some of the removed, collect for invalidation.
        for (actor in removed) {
            for (invalidated in depend.from(actor)) {
                depend.remove(actor, invalidated)
                collect(invalidated)
            }
        }

        // Second part, for all existing actors check if new actors are considered.
        for (actor in actors) if (actor !in added)
            for (source in added)
                if (source in actor.inDef.depends) {
                    depend.add(actor, source)
                    collect(actor)
                }

        // Third part, for all added actors add sources.
        for (actor in added) {
            val sources = actor.inDef.depends.all(this)
            for (source in sources) {
                depend.add(actor, source)
                collect(source)
            }
        }

        // Fourth part, add actor itself, will be invalid.
        for (actor in added)
            collect(actor)
    }

    /**
     * Collected for update of derivative.
     */
    private val collected by set<Actor<*, *>>()

    // Add actor to be updated.
    fun collect(actor: Actor<*, *>) {
        println("Update: $actor")
        collected.add(actor)
    }

    // Add dependents to be updated.
    fun collectDependent(actor: Actor<*, *>) {
        val elements = depend.from(actor)
        println("From $actor update: $elements")
        collected.addAll(elements)
    }

    /**
     * Runs a simulation step.
     */
    fun simulate(time: Double, dt: Double) {
        println("Sim start ...")

        // Clone updated set, clear base.
        val simulate = TreeSet(collected)
        collected.clear()

        // Iterate and update all entries that need derivative.
        for (update in simulate)
            update.derive(depend.from(update))

        // Simulate all actors.
        for (actor in actors) {
            actor.advance(time, dt)
        }

        println("sim end.")
    }

    /**
     * Periodic world update.
     */
    val worldUpdate = repeating(Short.MAX_VALUE, 1000, shell::initializedTime) {
        simulate(time.global / 1000.0, 1.0)
    }

    /**
     * Renders all actors.
     */
    fun render(time: Double, delta: Double) {
        for (actor in actors)
            actor.render(time, delta)
    }
}