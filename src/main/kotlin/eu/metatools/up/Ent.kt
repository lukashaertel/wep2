package eu.metatools.up

import eu.metatools.up.aspects.*
import eu.metatools.up.dt.*
import eu.metatools.up.lang.autoClosing
import eu.metatools.up.structure.Container
import eu.metatools.up.structure.Part
import java.util.*
import kotlin.reflect.*

abstract class Ent(on: Aspects?, override val id: Lx) : With(on), Container, Part {
    /**
     * Current execution's time.
     */
    private val executionTime = ThreadLocal<Time>()

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

    override fun resolve(id: Lx) =
        parts[id subtract this.id]

    override fun include(id: Lx, part: Part) {
        parts[id subtract this.id] = part

        // Connect the part if already running.
        if (connected)
            part.connect()
    }

    override fun exclude(id: Lx) {
        // Remove part.
        parts.remove(id)?.let {
            // If was removed and this is connected, disconnect the target.
            if (connected)
                it.disconnect()
        }
    }

    override fun connect() {
        // If dispatch is provided.
        this<Dispatch> {
            // Assign close.
            closeReceive = handlePerform.register(id) { perform(it) }
        }

        // Register saving if needed. Loading must be done externally, as existence is self dependent.
        this<Store> {
            // Register storing the entity to the primary entity table.
            closeSave = handleSave.register {
                // Get the arguments or use empty to signal default construction rules should be applied.
                val args = with<Args>()?.extraArgs.orEmpty()

                // Store to PET under the entities own identity.
                it(PET / id, this@Ent::class to args)
            }
        }

        // Connect in ascending order.
        parts.forEach { (_, part) ->
            part.connect()
        }

        // Mark connected.
        connected = true
    }

    protected fun <T : Ent> constructed(ent: T): T {
        // todo: Direct to proper root container.
        on<Container> {
            include(ent.id, ent)
            on<Track> {
                resetWith(presence / ent.id) {
                    exclude(ent.id)
                }
            }
        }

        return ent
    }

    override fun disconnect() {
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
        // Assign execution time.
        executionTime.set(instruction.time)

        // Invoke callable.
        dispatchTable[instruction.methodName.toInt()](instruction.args)

        // Reset execution time.
        executionTime.set(null)
    }

    /**
     * Instruction-out node. Called by dispatching wrappers.
     */
    private fun send(instruction: Instruction) {
        this<Dispatch> {
            send(id, instruction)
        }
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
        return { time -> send(Instruction(name, time, listOf())) }
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
        return { time, arg -> send(Instruction(name, time, listOf(arg))) }
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
        return { time, arg1, arg2 -> send(Instruction(name, time, listOf(arg1, arg2))) }
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
        return { time, arg1, arg2, arg3 -> send(Instruction(name, time, listOf(arg1, arg2, arg3))) }
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
        return { time, arg1, arg2, arg3, arg4 -> send(Instruction(name, time, listOf(arg1, arg2, arg3, arg4))) }
    }

}