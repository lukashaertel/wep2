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
     * Stores the per-ID reset method.
     */
    private val currentResets = hashMapOf<Lx, () -> Unit>()

    private val store = TreeMap<Time, Array<Any>>()

    init {
        this<Dispatch> {
            handlePrepare.register { _, i ->
                store.tailMap(i.time, true).descendingMap().values.forEach { (_, _, undo) ->
                    undo as () -> Unit
                    undo()
                }
            }

            handleComplete.register { k, i ->
                store[i.time] = arrayOf(k, i, compile())
                store.tailMap(i.time, false).values.forEach { a ->
                    val id = a[0] as Lx
                    val instruction = a[1] as Instruction
                    handlePerform(id, instruction)
                    a[2] = compile()
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