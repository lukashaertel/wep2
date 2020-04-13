package eu.metatools.sx.ents

import eu.metatools.sx.SX
import eu.metatools.up.Ent
import eu.metatools.up.Shell
import eu.metatools.up.dsl.propObserved
import eu.metatools.up.dt.Lx
import java.util.*

/**
 * Actor definition.
 */
interface DefActor<A, D> {
    /**
     * Actor dependence query.
     */
    val depends: Query

    /**
     * Default delta pre-initialization.
     */
    val deltaZero: D

    /**
     * Creates the derivative, the dependent actors have changed, init occurred or an edge state was reached.
     * @param state The current state.
     * @param depends The dependant actors.
     * @return Returns the derivative.
     */
    fun derive(state: A, depends: SortedSet<Actor<*, *>>): D

    /**
     * Applies a [derivative] delta to the [state] at the [time] and span [dt].
     * @param state The state to apply to.
     * @param derivative The derivative to apply.
     * @param time The current time if needed.
     * @param dt The delta time span between simulations.
     * @return Returns the new absolute state.
     */
    fun advance(state: A, derivative: D, time: Double, dt: Double): A

    /**
     * True if a state is an edge state, causing a reevaluation of the delta.
     * @param state The state to evaluate.
     */
    fun isEdge(state: A, derivative: D): Boolean
}

interface VisActor<A, D> {
    fun render(state: A, derivative: D, time: Double, dt: Double)
}


class Actor<A, D>(
    shell: Shell,
    id: Lx,
    val sx: SX,
    val inState: A,
    val inDef: DefActor<A, D>,
    val inVis: VisActor<A, D>?
) : Ent(shell, id) {
    override val extraArgs = mapOf(
        "inState" to inState,
        "inDef" to inDef,
        "inVis" to inVis
    )

    /**
     * Current status of the actor, can be a stable state. If state is changed,
     */
    var state by propObserved({ inState }, inState) { (from, to) ->
        if (from != to) sx.root.collectDependent(this)
    }
        private set

    /**
     * Derivative, used in state advance.
     */
    var delta by propObserved({ inDef.deltaZero }, inDef.deltaZero) { (from, to) ->
        // Delta changed, update.
        if (from != to) sx.root.collectDependent(this)
    }
        private set

    /**
     * Updates the delta.
     */
    fun derive(depends: SortedSet<Actor<*, *>>) {
        // Derive delta for state and depends.
        delta = inDef.derive(state, depends)
    }

    /**
     * Advances (or retains) the state.
     */
    fun advance(time: Double, dt: Double) {
        // Advance state.
        state = inDef.advance(state, delta, time, dt)

        // Collect for update.
        if (inDef.isEdge(state, delta))
            sx.root.collect(this)
    }

    fun render(time: Double, dt: Double) {
        inVis?.render(state, delta, time, dt)
    }
}