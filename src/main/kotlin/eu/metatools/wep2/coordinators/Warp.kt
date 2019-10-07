package eu.metatools.wep2.coordinators

import java.util.*

/**
 * A coordinator handling equivalent state restoration by weaving in new instructions, undoing and replaying
 * the temporarily invalidated instructions.
 *
 * For example, weaving `(-, 2, -)` into `(-, 1, -), (-, 3, -)`, [Warp] will do the following.
 *
 * * Undo `(-, 3, -)`
 * * Insert and do `(-, 2, -)`
 * * Redo `(-, 3, -)`
 *
 * The instruction `(-, 1, -)` is left untouched.
 */
abstract class Warp<N, T : Comparable<T>> : BaseCoordinator<N, T>() {
    /**
     * An instruction that has been executed as part of direction.
     * @on name The name that was executed.
     * @on args The arguments of the execution.
     * @on undo The associated undo operation, this field will be mutated once
     * an instruction is reevaluated.
     */
    private data class Instruction<N, T : Comparable<T>>(
        val name: N,
        val args: Any?,
        var undo: () -> Unit
    )

    /**
     * All instructions and their undo operations.
     */
    private val instructionCache = TreeMap<T, Instruction<N, T>>()

    /**
     * All name/time/argument triples.
     */
    val instructions: List<Triple<N, T, Any?>>
        get() = instructionCache.map { (t, i) -> Triple(i.name, t, i.args) }

    /**
     * Evaluates the function identified by it's [name] at [time] with the [args], returning
     * the undo of the action.
     * @param name The name of what to invoke.
     * @param time The time at which to evaluate.
     * @param args All arguments.
     */
    abstract fun evaluate(name: N, time: T, args: Any?): () -> Unit

    /**
     * Consolidate all instructions, reducing overhead for instructions that
     * are known not to be undone anymore.
     * @param time The time up to which to consolidate. Instructions *exactly on* that
     * time can still be undone.
     */
    open fun consolidate(time: T) {
        instructionCache
            .headMap(time, false)
            .clear()
    }

    /**
     * Undoes every instruction in the local instruction cache.
     */
    fun undoAll() {
        instructionCache
            .descendingMap()
            .forEach { (_, u) -> u.undo() }
    }

    /**
     * Redoes every instruction in the local instruction cache.
     */
    fun redoAll() {
        instructionCache.forEach { (t, u) ->
            u.undo = evaluate(u.name, t, u.args)
        }
    }

    /**
     * Receives a single remote invocation to be chained.
     * @param name The name of what to invoke.
     * @param time The time at which to evaluate.
     * @param args All arguments.
     */
    override fun receive(name: N, time: T, args: Any?) {
        // Get instructions after the desired time.
        val part = instructionCache.tailMap(time, true)

        // Undo existing instructions in reverse order.
        part.descendingMap()
            .forEach { (_, u) -> u.undo() }

        // Add newly received instruction with it's undo operation (assert it was unmapped).
        part.put(time, Instruction(name, args, evaluate(name, time, args))).let {
            check(it == null) { "Time slot $time is already occupied by $it" }
        }

        // Reevaluate the remaining instructions and reassign undo operations.
        part.tailMap(time, false)
            .forEach { (t, u) -> u.undo = evaluate(u.name, t, u.args) }
    }

    /**
     * Receives multiple remote invocations to be chained.
     * @param triples A list of triples consisting of name, time and arguments
     * of an invocation.
     */
    override fun receiveAll(triples: Sequence<Triple<N, T, Any?>>) {
        // Associate the calls by their time, do nothing if empty.
        val associated = triples
            .associateByTo(TreeMap()) { it.second }
            .takeIf { it.isNotEmpty() } ?: return

        // Reduce instructions to those actually invalidated.
        val part = instructionCache.tailMap(associated.firstKey(), true)

        // Undo those instructions in reverse order.
        part.descendingMap()
            .forEach { (_, u) -> u.undo() }

        // Fold over segments between calls to be inserted.
        val last = triples
            .sortedBy { it.second }
            .fold(associated.firstKey()) { from, (name, time, args) ->
                // Redo the existing instructions.
                part.subMap(from, false, time, false)
                    .forEach { (t, u) -> u.undo = evaluate(u.name, t, u.args) }

                // Insert the new instruction while asserting slot was unoccupied.
                part.put(time, Instruction(name, args, evaluate(name, time, args))).let {
                    check(it == null) { "Time slot $time is already occupied by $it" }
                }

                // The next step will start after the currently inserted time.
                time
            }

        // Redo instructions after the last segment.
        part.tailMap(last, false)
            .forEach { (t, u) -> u.undo = evaluate(u.name, t, u.args) }
    }
}

