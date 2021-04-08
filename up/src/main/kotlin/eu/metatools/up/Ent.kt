package eu.metatools.up

import eu.metatools.up.dt.*
import eu.metatools.up.lang.frequencyProgression
import java.util.*

/**
 * Base class for exchanged entity in a [shell].
 * @property shell The [Shell], must actually implement [Engine] but to guard from invoking system routing and
 * administration options, only the shell type is passed.
 * @property id The unique identity of the entity.
 */
abstract class Ent(val shell: Shell, val id: Lx) : Comparable<Ent> {
    companion object {
        /**
         * Current execution's name.
         */
        private val executionMethod = ThreadLocal<MethodName>()

        /**
         * Current execution's time.
         */
        private val executionTime = ThreadLocal<Time>()

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
    val driver = shell.engine.amend(object : Driver {
        override val ent: Ent
            get() = this@Ent

        override var isConnected = false
            private set

        /**
         * Adds a part to the entity. This maintains connection status and allows resolution. Automatically performed by
         * the property creators and DSL methods.
         */
        override fun configure(part: Part) {
            requireUnconnected()

            parts[part.name] = part
            part.notifyHandle = { name, change -> ent.partChanged(name, change) }
        }

        /**
         * Connects this entity. Automatically called on [constructed] and [delete].
         */
        override fun connect(entIn: EntIn?) {
            requireUnconnected()

            // Connect in ascending order.
            if (entIn != null)
                parts.forEach { (name, part) ->
                    part.connect { key -> entIn(name, key) }
                }
            else
                parts.forEach { (_, part) ->
                    part.connect(null)
                }

            // Mark connected.
            isConnected = true

            // Invoke post-connect handler.
            postConnect()
        }

        override fun persist(entOut: EntOut) {
            requireConnected()

            parts.forEach { (name, part) ->
                part.persist { key, value -> entOut(name, key, value) }
            }
        }

        /**
         * Disconnects this entity. Automatically called on [constructed] and [delete].
         */
        override fun disconnect() {
            requireConnected()

            // Invoke pre-disconnect handler.
            preDisconnect()

            // Mark not connected.
            isConnected = false

            // Stop in descending order.
            parts.descendingMap().forEach { (_, part) ->
                part.disconnect()
            }
        }

        /**
         * Instruction-in node. Called by registered handlers.
         */
        override fun perform(instruction: Instruction) {
            requireConnected()

            // Check if fresh invoke.
            require(executionTime.get() == null) {
                "Running in open exchanged callback."
            }

            // Push execution parameters.
            executionTime.set(instruction.time)
            executionMethod.set(instruction.methodName)

            // Invoke callable.
            dispatchTable[instruction.methodName.toInt()](instruction.args)

            // Reset execution parameters.
            executionMethod.set(null)
            executionTime.set(null)
        }

        override fun ready() {
            requireConnected()

            // Ready on each part.
            parts.forEach { (_, part) ->
                part.ready()
            }
        }
    })

    /**
     * Local dispatch table.
     */
    private val dispatchTable = mutableListOf<(List<Any?>) -> Unit>()

    /**
     * The parts of the entity.
     */
    private val parts = TreeMap<String, Part>()

    /**
     * The time at which the current instruction is evaluated.
     */
    protected val time: Time get() = executionTime.get()

    /**
     * The currently executing method name.
     */
    protected val method: MethodName get() = executionMethod.get()

    /**
     * Provides extra arguments for entity construction by name and value. **Do not pass references here or use values
     * outside of initialization.**
     */
    open val extraArgs: Map<String, Any?>? get() = null

    /**
     * Returns a [Lx] composed of the entity's [id], the currently executing [method] and the [time]. If an exchanged
     * method creates a single entity, this value can used as is to create a child. Multiple children should be further
     * specialized by appending another node, i.e., by a number locally incrementing for the call.
     */
    protected fun newId() =
            id / method / time

    /**
     * Gets the fractional seconds since the shell was initialized.
     */
    protected val elapsed
        get() = (time.global - shell.initializedTime) / 1000.0

    /**
     * Marks that an entity has been constructed. This will add the entity to the tracking of the [Engine] and deal with
     * undoing tracking on resets.
     */
    protected fun <T : Ent> constructed(ent: T): T {
        // Include in scope.
        shell.engine.add(ent)

        // Undo by excluding.
        shell.engine.capture {
            shell.engine.remove(ent)
        }

        return ent
    }

    /**
     * Marks that an entity has been deleted. This will remove the entity from the tracking of the [Engine] and deal
     * with redoing tracking on resets.
     */
    protected fun delete(ent: Ent) {
        // Exclude from scope.
        shell.engine.remove(ent)

        // Undo by adding.
        shell.engine.capture {
            shell.engine.add(ent)
        }
    }

    /**
     * Invoked after property connection.
     */
    protected open fun postConnect() = Unit

    /**
     * Invoked before property disconnection.
     */
    protected open fun preDisconnect() = Unit

    protected open fun partChanged(name: String, change: Change<*>) = Unit

    /**
     * Creates an exchanged send/perform wrapper for the function.
     */
    @JvmName("exchangedFunction0")
    protected fun exchange(function: () -> Any?)
            : (Time) -> Unit {
        // Mark errors.
        require(!driver.isConnected) { "Exchanging in connected entity, this is most likely an invalid call." }

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
        require(!driver.isConnected) { "Exchanging in connected entity, this is most likely an invalid call." }

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
        require(!driver.isConnected) { "Exchanging in connected entity, this is most likely an invalid call." }

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
        require(!driver.isConnected) { "Exchanging in connected entity, this is most likely an invalid call." }

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
        require(!driver.isConnected) { "Exchanging in connected entity, this is most likely an invalid call." }

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
            player: Short,
            frequency: Long,
            init: () -> Long,
            function: () -> Unit
    ): (Long) -> Unit {
        // Mark errors.
        require(!driver.isConnected) { "Marking repeating in connected entity, this is most likely an invalid call." }

        // Resolve name.
        val name = dispatchTable.size.toMethodName()

        // Add to dispatch table.
        dispatchTable.add {
            @Suppress("unchecked_cast")
            (function())
        }

        var initial = Long.MAX_VALUE
        var rdc = Byte.MIN_VALUE

        // Get ticker name for storing and loading.
        val tickerId = "tickers-$name"

        // Include a part handling store aspects.
        driver.configure(object : Part {
            override var isConnected = false
                private set

            override val name: String
                get() = tickerId

            override var notifyHandle: (String, Change<*>) -> Unit
                get() = throw UnsupportedOperationException()
                set(_) {}

            override fun connect(partIn: PartIn?) {
                // Restore last value if loading.
                if (partIn != null) {
                    initial = partIn("initial") as Long
                    rdc = partIn("rdc") as Byte
                } else {
                    initial = init()
                    rdc = repeatingDC++
                }

                isConnected = true
            }

            override fun persist(partOut: PartOut) {
                partOut("initial", initial)
                partOut("rdc", rdc)
            }

            override fun disconnect() {
                isConnected = false
            }

            override fun ready() {
                // Nothing.
            }

            override fun toString() =
                    "<repeating, ~$frequency - $initial>"
        })

        // Return non-exchanged local invocation.
        return { time ->
            val start = shell.engine.lastRepeatingTime(player, rdc)?.inc() ?: initial

            // Generate local instructions.
            val locals = frequencyProgression(initial, frequency, start, time).map {
                Instruction(id, name, Time(it, player, rdc), emptyList())
            }

            // Call unsafe local receive.
            if (locals.isNotEmpty())
                shell.engine.local(locals.asSequence())
        }
    }


    /**
     * Returns a random number generator valid for the executing fragment.
     */
    fun rng(): Random {
        val (g, _, l) = time
        return Random(g xor l.toLong())
    }

    override fun compareTo(other: Ent) =
            id.compareTo(other.id)

    override fun toString() =
            extraArgs.let {
                if (it.isNullOrEmpty())
                    "${this::class.simpleName}#$id"
                else
                    "${this::class.simpleName}#$id $it"
            }
}

/**
 * True if the receiver is an entity and connected.
 *
 * @since Entities might be deleted and still referred to in frontend. This will not be necessary in future revisions.
 */
fun Any.isConnected() =
        this !is Ent || driver.isConnected
