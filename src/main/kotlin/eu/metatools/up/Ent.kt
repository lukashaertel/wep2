package eu.metatools.up

import eu.metatools.up.dt.*
import eu.metatools.up.lang.autoClosing
import eu.metatools.up.lang.frequencyProgression
import java.io.PrintStream
import java.io.StringWriter
import java.io.Writer
import java.lang.Appendable
import java.util.*
import kotlin.experimental.inv

/**
 * Base class for exchanged entity in a [shell].
 * @property shell The [Shell], must actually implement [Engine] but to guard from invoking system routing and
 * administration options, only the shell type is passed.
 * @property id The unique identity of the entity.
 */
abstract class Ent(val shell: Shell, val id: Lx) : Comparable<Ent> {
    companion object {
        /**
         * Current execution's time.
         */
        private val executionTime = ThreadLocal<Time>()


        /**
         * Unique ID of the driver domain. Do not use this key as a root node.
         */
        private val driverDomain = ".dd"

        /**
         * Presence attribute.
         */
        private val presence = lx / driverDomain / "EX"

        /**
         * Disambiguation code for [repeating]. // TODO: Maybe research if pulling more data from instruction can help
         * auto-disambiguation.
         */
        @Deprecated("Unverified global, temporary solution.")
        private var repeatingDC = Byte.MIN_VALUE
    }

    /**
     * Entity driver. These operations are used by [Engine]s and private methods to properly link up the entity without
     * exposing critical API.
     */
    val driver = object : Driver {
        /**
         * Adds a part to the entity. This maintains connection status and allows resolution. Automatically performed by
         * the property creators and DSL methods.
         */
        override fun include(id: Lx, part: Part) {
            parts[id subtract this@Ent.id] = part

            // Connect the part if already running.
            if (connected)
                part.connect()
        }

        /**
         * Connects this entity. Automatically called on [constructed] and [delete].
         */
        override fun connect() {
            // Register detached perform.
            closeReceive = shell.engine.onPerform.register(id) { perform(it) }

            // Connect in ascending order.
            parts.forEach { (_, part) ->
                part.connect()
            }

            // Mark connected.
            connected = true
        }

        /**
         * Disconnects this entity. Automatically called on [constructed] and [delete].
         */
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
        override fun perform(instruction: Instruction) {
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

        override fun cat(appendable: Appendable) {
            // Header.
            appendable.append(Ent::class.simpleName)
            appendable.append(": ")
            appendable.append(id.toString())

            // Extra args.
            extraArgs?.let {
                appendable.append(it.toString())
            }

            // Print detached.
            if (connected)
                appendable.appendln()
            else
                appendable.appendln(" (detached)")

            // List parts.
            for ((id, part) in parts) {
                appendable.append("  ")
                appendable.append(id.toString())
                appendable.append(": ")
                appendable.appendln(part.toString())
            }
        }
    }

    /**
     * Local dispatch table.
     */
    private val dispatchTable = mutableListOf<(List<Any?>) -> Unit>()

    /**
     * Receiver connection, automatically closed on reassign.
     */
    private var closeReceive by autoClosing()

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

    /**
     * Provides extra arguments for entity construction by name and value. **Do not pass references here or use values
     * outside of initialization.**
     */
    open val extraArgs: Map<String, Any?>? get() = null


    /**
     * Marks that an entity has been constructed. This will add the entity to the tracking of the [Engine] and deal with
     * undoing tracking on resets.
     */
    protected fun <T : Ent> constructed(ent: T): T {
        // Include in scope.
        shell.engine.add(ent)

        // Undo by excluding.
        shell.engine.capture(presence / ent.id) {
            shell.engine.remove(ent.id)
        }

        return ent
    }

    /**
     * Marks that an entity has been deleted. This will remove the entity from the tracking of the [Engine] and deal
     * with redoing tracking on resets.
     */
    protected fun delete(ent: Ent) {
        // Exclude from scope.
        shell.engine.remove(ent.id)

        // Undo by adding.
        shell.engine.capture(presence / ent.id) {
            shell.engine.add(ent)
        }
    }

    /**
     * Creates an exchanged send/perform wrapper for the function.
     */
    @JvmName("exchangedFunction0")
    protected fun exchange(function: () -> Any?)
            : (Time) -> Unit {
        // Mark errors.
        require(!connected) { "Exchanging in connected entity, this is most likely an invalid call." }

        // Resolve name and add to dispatch table.
        val name = dispatchTable.size.toMethodName()
        dispatchTable.add { function() }

        // Return send invocation.
        return { time -> shell.engine.exchange(Instruction(id, name, time, listOf())) }
    }

    /**
     * Creates an exchanged send/perform wrapper for the function.
     */
    @JvmName("exchangedFunction1")
    protected fun <T> exchange(function: (T) -> Any?)
            : (Time, T) -> Unit {
        // Mark errors.
        require(!connected) { "Exchanging in connected entity, this is most likely an invalid call." }

        // Resolve name.
        val name = dispatchTable.size.toMethodName()

        // Add to dispatch table.
        dispatchTable.add { (arg) ->
            @Suppress("unchecked_cast")
            (function(arg as T))
        }

        // Return send invocation.
        return { time, arg -> shell.engine.exchange(Instruction(id, name, time, listOf(arg))) }
    }

    /**
     * Creates an exchanged send/perform wrapper for the function.
     */
    @JvmName("exchangedFunction2")
    protected fun <T, U> exchange(function: (T, U) -> Any?)
            : (Time, T, U) -> Unit {
        // Mark errors.
        require(!connected) { "Exchanging in connected entity, this is most likely an invalid call." }

        // Resolve name.
        val name = dispatchTable.size.toMethodName()

        // Add to dispatch table.
        dispatchTable.add { (arg1, arg2) ->
            @Suppress("unchecked_cast")
            (function(arg1 as T, arg2 as U))
        }

        // Return send invocation.
        return { time, arg1, arg2 -> shell.engine.exchange(Instruction(id, name, time, listOf(arg1, arg2))) }
    }

    /**
     * Creates an exchanged send/perform wrapper for the function.
     */
    @JvmName("exchangedFunction3")
    protected fun <T, U, V> exchange(function: (T, U, V) -> Any?)
            : (Time, T, U, V) -> Unit {
        // Mark errors.
        require(!connected) { "Exchanging in connected entity, this is most likely an invalid call." }

        // Resolve name.
        val name = dispatchTable.size.toMethodName()

        // Add to dispatch table.
        dispatchTable.add { (arg1, arg2, arg3) ->
            @Suppress("unchecked_cast")
            (function(arg1 as T, arg2 as U, arg3 as V))
        }

        // Return send invocation.
        return { time, arg1, arg2, arg3 ->
            shell.engine.exchange(
                Instruction(
                    id,
                    name,
                    time,
                    listOf(arg1, arg2, arg3)
                )
            )
        }
    }

    /**
     * Creates an exchanged send/perform wrapper for the function.
     */
    @JvmName("exchangedFunction4")
    protected fun <T, U, V, W> exchange(function: (T, U, V, W) -> Any?)
            : (Time, T, U, V, W) -> Unit {
        // Mark errors.
        require(!connected) { "Exchanging in connected entity, this is most likely an invalid call." }

        // Resolve name.
        val name = dispatchTable.size.toMethodName()

        // Add to dispatch table.
        dispatchTable.add { (arg1, arg2, arg3, arg4) ->
            @Suppress("unchecked_cast")
            (function(arg1 as T, arg2 as U, arg3 as V, arg4 as W))
        }

        // Return send invocation.
        return { time, arg1, arg2, arg3, arg4 ->
            shell.engine.exchange(
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

    /**
     * Creates an non-exchanged wrapper that from the initial value to the currently passed value invokes the method
     * given by [function].
     *
     * E.g., say initial is `0` and frequency is `10`. If the last time this method was invoked
     * is `15`, it generated `0, 10`. A following call passing `40` would generate calls at `20, 30`. Note: the passed
     * value is excluded.
     *
     * @param frequency The frequency of times to generate.
     * @param init The initial time generator, might be linked to retrieving the [Shell.initializedTime] or a property
     * remembering when the [Ent] was constructed.
     * @param function The function to run. Itself will have [time] properly assigned during it's invocation.
     */
    protected fun repeating(
        frequency: Long,
        init: () -> Long,
        function: () -> Any?
    ): (Long) -> Unit {
        // Mark errors.
        require(!connected) { "Marking repeating in connected entity, this is most likely an invalid call." }

        // Resolve name.
        val name = dispatchTable.size.toMethodName()

        // Add to dispatch table.
        dispatchTable.add {
            @Suppress("unchecked_cast")
            (function())
        }

        var initial = Long.MAX_VALUE
        var last = Long.MAX_VALUE
        var rdc = Byte.MIN_VALUE

        // Get ticker name for storing and loading.
        val tickerId = id / "tickers-$name"

        // Include a part handling store aspects.
        driver.include(tickerId, object : Part {
            /**
             * Close handle for store connection.
             */
            var closeSave by autoClosing()

            override fun connect() {
                // Restore last value if loading.
                if (shell.engine.mode == Mode.RestoreData) {
                    initial = shell.engine.load(tickerId / "initial") as Long
                    last = shell.engine.load(tickerId / "last") as Long
                    rdc = shell.engine.load(tickerId / "rdc") as Byte
                } else {
                    initial = init()
                    last = init()
                    rdc = repeatingDC++
                }

                // Save by writing last.
                closeSave = shell.engine.onSave.register {
                    shell.engine.save(tickerId / "initial", initial)
                    shell.engine.save(tickerId / "last", last)
                    shell.engine.save(tickerId / "rdc", rdc)
                }
            }

            override fun disconnect() {
                closeSave = null
            }

            override fun toString() =
                "<repeating, ~$frequency - $initial>"
        })

        // Return non-exchanged local invocation.
        return { time ->
            if (time > last) {
                // Generate local instructions.
                val locals = frequencyProgression(initial, frequency, last, time).asSequence().map {
                    Instruction(id, name, Time(it, shell.player.inv(), rdc), emptyList())
                }

                // Transfer last time.
                last = time

                // Call unsafe local receive.
                shell.engine.local(locals)
            }
        }
    }

    override fun compareTo(other: Ent) =
        id.compareTo(other.id)

    override fun toString() =
        // Print to a string writer, return value.
        StringWriter().also(driver::cat).toString()
}