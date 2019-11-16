package eu.metatools.up.basic

import eu.metatools.up.aspects.*
import eu.metatools.up.dt.Instruction
import eu.metatools.up.dt.Lx

data class Commit(val views: Set<Lx>, val changes: Map<Lx, Change<*>>)

/**
 * Tracks changes and views per ID.
 */
class CommitListener(
    on: Aspects?,
    val emit: (Lx, Instruction, Commit) -> Unit
) : With(on), Listen {
    /**
     * Current views.
     */
    private val currentViews = hashSetOf<Lx>()

    /**
     * Current changes.
     */
    private val currentChanges = hashMapOf<Lx, Change<*>>()

    init {
        this<Dispatch> {
            handlePrepare.register { _, _ ->
                currentViews.clear()
                currentChanges.clear()
            }
            handleComplete.register { key, arg ->
                emit(key, arg, Commit(currentViews.toSet(), currentChanges.toMap()))
            }
        }
    }

    override fun viewed(id: Lx, value: Any?) {
        currentViews.add(id)
    }

    override fun changed(id: Lx, change: Change<*>) {
        currentChanges.compute(id) { _, existing ->
            existing?.mergeForce(change) ?: change
        }
    }
}