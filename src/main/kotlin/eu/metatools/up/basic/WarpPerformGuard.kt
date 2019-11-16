package eu.metatools.up.basic

import eu.metatools.up.aspects.*
import eu.metatools.up.dt.Instruction
import eu.metatools.up.dt.Lx
import eu.metatools.up.dt.Time
import java.util.*

/**
 * Tracks undos per ID.
 */
class WarpPerformGuard(on: Aspects?) : With(on), Track {
    /**
     * [WarpPerformGuard] entry.
     * @property id The ID to run on.
     * @property instruction The instruction to run.
     * @property undo The currently assigned undo value.
     */
    private data class GuardEntry(val id: Lx, val instruction: Instruction, var undo: () -> Unit)

    /**
     * Stores the per-ID reset method.
     */
    private val currentResets = hashMapOf<Lx, () -> Unit>()

    /**
     * The store of guard entries per time slot.
     */
    private val store = TreeMap<Time, GuardEntry>()

    init {
        this<Dispatch> {
            handlePrepare.register { _, i ->
                // Undo in reverse order.
                store.tailMap(i.time, true).descendingMap().values.forEach { entry ->
                    // Undo performed guard entry.
                    entry.undo()
                }
            }

            handleComplete.register { k, i ->
                // Put new slot assignment, assert was empty.
                store.put(i.time, GuardEntry(k, i, compile()))?.let {
                    error("Slot already ${i.time} already occupied by $it")
                }

                // Re-execute from after execution.
                store.tailMap(i.time, false).values.forEach { entry ->
                    // Re-perform guard entry.
                    handlePerform(entry.id, entry.instruction)

                    // Overwrite undo.
                    entry.undo = compile()
                }
            }
        }
    }

    override fun resetWith(id: Lx, undo: () -> Unit) {
        currentResets.putIfAbsent(id, undo)
    }

    /**
     * Compiles the current primary reset method and clears the backings.
     */
    private fun compile(): () -> Unit {
        // Clone values, clear the map.
        val resets = currentResets.values.toList()
        currentResets.clear()

        // Return invoking all functions in sequence.
        return {
            for (reset in resets)
                reset()
        }
    }
}