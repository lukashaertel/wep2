package eu.metatools.up

import eu.metatools.up.dt.*
import eu.metatools.up.lang.autoClosing
import eu.metatools.up.dsl.Part
import eu.metatools.up.lang.frequencyProgression
import java.util.*

/**
 * Base class for exchanged entity in [scope].
 */
abstract class Ent(val scope: Scope, val id: Lx) {
    companion object {
        /**
         * Current execution's time.
         */
        private val executionTime = ThreadLocal<Time>()
    }

    /**
     * Provides extra arguments for entity construction by name and value.
     */
    open val extraArgs: Map<String, Any?>? get() = null

    /**
     * Local dispatch table.
     */
    private val dispatchTable = mutableListOf<(List<Any?>) -> Unit>()

    /**
     * Receiver connection, automatically closed on reassign.
     */
    private var closeReceive by autoClosing()

    /**
     * Store connection, automatically closed on reassign.
     */
    private var closeSave by autoClosing()

    /**
     * The parts of the entity.
     */
    private val parts = TreeMap<Lx, Part>()

    /**
     * True if the container is connected, invocation of [include] will connect the received part.
     */
    private var connected = false

    /**
     * The time at which the current instruction is evaluated.
     */
    protected val time: Time get() = executionTime.get()

    fun include(id: Lx, part: Part) {
        parts[id subtract this.id] = part

        // Connect the part if already running.
        if (connected)
            part.connect()
    }

    fun exclude(id: Lx) {
        // Remove part.
        parts.remove(id)?.let {
            // If was removed and this is connected, disconnect the target.
            if (connected)
                it.disconnect()
        }
    }

    fun connect() {
        // Register detached perform.
        closeReceive = scope.onPerform.register(id) { perform(it) }

        // Connect in ascending order.
        parts.forEach { (_, part) ->
            part.connect()
        }

        // Mark connected.
        connected = true
    }

    protected fun <T : Ent> constructed(ent: T): T {
        // Include in scope.
        scope.include(ent)

        // Undo by excluding.
        scope.capture(presence / ent.id) {
            scope.exclude(ent.id)
        }

        return ent
    }

    protected fun delete(ent: Ent) {
        // Exclude from scope.
        scope.exclude(ent.id)

        // Undo by adding.
        scope.capture(presence / ent.id) {
            scope.include(ent)
        }
    }

    fun disconnect() {
        // Mark not connected.
        connected = false

        // Stop in descending order.
        parts.descendingMap().forEach { (_, part) ->
            part.disconnect()
        }

        // Reset close, will close open connection via delegate.
        closeReceive = null
    }

    /**
     * Instruction-in node. Called by registered handlers.
     */
    private fun perform(instruction: Instruction) {
        // Check if fresh invoke.
        require(executionTime.get() == null) {
            "Running in open exchanged callback."
        }

        // Push execution time.
        executionTime.set(instruction.time)

        // Invoke callable.
        dispatchTable[instruction.methodName.toInt()](instruction.args)

        // Reset execution time.
        executionTime.set(null)
    }

    /**
     * Creates an exchanged send/perform wrapper for the function.
     */
    @JvmName("exchangedFunction0")
    protected fun exchange(function: () -> Any?)
            : (Time) -> Unit {
        // Resolve name and add to dispatch table.
        val name = dispatchTable.size.toMethodName()
        dispatchTable.add { function() }

        // Return send invocation.
        return { time -> scope.exchange(Instruction(id, name, time, listOf())) }
    }

    /**
     * Creates an exchanged send/perform wrapper for the function.
     */
    @JvmName("exchangedFunction1")
    protected fun <T> exchange(function: (T) -> Any?)
            : (Time, T) -> Unit {
        // Resolve name.
        val name = dispatchTable.size.toMethodName()

        // Add to dispatch table.
        dispatchTable.add { (arg) ->
            @Suppress("unchecked_cast")
            (function(arg as T))
        }

        // Return send invocation.
        return { time, arg -> scope.exchange(Instruction(id, name, time, listOf(arg))) }
    }

    /**
     * Creates an exchanged send/perform wrapper for the function.
     */
    @JvmName("exchangedFunction2")
    protected fun <T, U> exchange(function: (T, U) -> Any?)
            : (Time, T, U) -> Unit {
        // Resolve name.
        val name = dispatchTable.size.toMethodName()

        // Add to dispatch table.
        dispatchTable.add { (arg1, arg2) ->
            @Suppress("unchecked_cast")
            (function(arg1 as T, arg2 as U))
        }

        // Return send invocation.
        return { time, arg1, arg2 -> scope.exchange(Instruction(id, name, time, listOf(arg1, arg2))) }
    }

    /**
     * Creates an exchanged send/perform wrapper for the function.
     */
    @JvmName("exchangedFunction3")
    protected fun <T, U, V> exchange(function: (T, U, V) -> Any?)
            : (Time, T, U, V) -> Unit {
        // Resolve name.
        val name = dispatchTable.size.toMethodName()

        // Add to dispatch table.
        dispatchTable.add { (arg1, arg2, arg3) ->
            @Suppress("unchecked_cast")
            (function(arg1 as T, arg2 as U, arg3 as V))
        }

        // Return send invocation.
        return { time, arg1, arg2, arg3 -> scope.exchange(Instruction(id, name, time, listOf(arg1, arg2, arg3))) }
    }

    /**
     * Creates an exchanged send/perform wrapper for the function.
     */
    @JvmName("exchangedFunction4")
    protected fun <T, U, V, W> exchange(function: (T, U, V, W) -> Any?)
            : (Time, T, U, V, W) -> Unit {
        // Resolve name.
        val name = dispatchTable.size.toMethodName()

        // Add to dispatch table.
        dispatchTable.add { (arg1, arg2, arg3, arg4) ->
            @Suppress("unchecked_cast")
            (function(arg1 as T, arg2 as U, arg3 as V, arg4 as W))
        }

        // Return send invocation.
        return { time, arg1, arg2, arg3, arg4 ->
            scope.exchange(
                Instruction(
                    id,
                    name,
                    time,
                    listOf(arg1, arg2, arg3, arg4)
                )
            )
        }
    }

    // TODO: Full verification necessary.
    // TODO: Initial as generator not nice.
    protected fun repeating(
        frequency: Long,
        init: () -> Long,
        player: Short = Short.MAX_VALUE,
        local: Byte = Byte.MIN_VALUE,
        function: () -> Any?
    ): (Long) -> Unit {
        // Resolve name.
        val name = dispatchTable.size.toMethodName()

        // Add to dispatch table.
        dispatchTable.add {
            @Suppress("unchecked_cast")
            (function())
        }

        var initial: Long = Long.MAX_VALUE
        var last: Long = Long.MAX_VALUE

        // Get ticker name for storing and loading.
        val tickerId = id / ".tickers" / name

        // Include a part handling store aspects.
        include(tickerId, object : Part {
            /**
             * Close handle for store connection.
             */
            var closeSave by autoClosing()

            override fun connect() {
                // Restore last value if loading.
                if (scope.mode == Mode.RestoreData) {
                    initial = scope.load(tickerId / "initial") as Long
                    last = scope.load(tickerId / "last") as Long
                } else {
                    initial = init()
                    last = init()
                }

                // Save by writing last.
                closeSave = scope.onSave.register {
                    scope.save(tickerId / "initial", initial)
                    scope.save(tickerId / "last", last)
                }
            }

            override fun disconnect() {
                closeSave = null
            }
        })

        // Return non-exchanged local invocation.
        return { time ->
            // Generate local instructions.
            val locals = frequencyProgression(initial, frequency, last, time).asSequence().map {
                // TODO: This is bound to be permafucked.
                Instruction(id, name, Time(it, player, local), emptyList())
            }

            // Transfer last time.
            last = time

            // Call unsafe local receive.
            scope.local(locals)
        }
    }
}