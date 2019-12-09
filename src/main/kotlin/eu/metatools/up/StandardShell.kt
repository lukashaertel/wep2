package eu.metatools.up

import eu.metatools.up.dt.*
import eu.metatools.up.lang.constructBy
import eu.metatools.up.lang.never
import eu.metatools.up.lang.validate
import eu.metatools.up.notify.*
import java.util.*
import kotlin.collections.HashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.cast
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSupertypeOf

/**
 * Mode of the engine.
 */
private enum class Mode {
    /**
     * In between actual operations.
     */
    Idle,
    /**
     * Restoring from fields.
     */
    RestoreData,

    /**
     * Storing to fields.
     */
    StoreData,

    /**
     * Revoking instructions.
     */
    Revoke,

    /**
     * Invoking instructions.
     */
    Invoke
}


/**
 * Standard shell and engine with bound player and proxy nodes for incoming and outgoing instructions.
 */
class StandardShell(player: Short, val synchronized: Boolean = true) : Shell {
    companion object {
        /**
         * Type of the [Shell], used for restore.
         */
        private val scopeType = Shell::class.createType()

        /**
         * Type of the [Lx], used for restore.
         */
        private val lxType = Lx::class.createType()

        /**
         * Default, exception throwing load handler.
         */
        private val defaultLoadFromHandler: (Lx) -> Nothing = {
            error("No load handler assigned.")
        }

        /**
         * Default, exception throwing save handler.
         */
        private val defaultSaveToHandler: (Lx, Any?) -> Unit = { _, _ ->
            error("No save handler assigned.")
        }

        /**
         * Unique ID of the engine domain. Do not use this key as a root node.
         */
        private val engineDomain = ".ed"

        /**
         * System initialized time.
         */
        private val SIT = lx / engineDomain / "SIT"

        /**
         * Primary entity table.
         */
        private val PET = lx / engineDomain / "PET"

        /**
         * Local time per global.
         */
        private val LPG = lx / engineDomain / "LPG"

        /**
         * Instruction replay table.
         */
        private val IRT = lx / engineDomain / "IRT"
    }

    /**
     * Timed instruction registration entry with associated local undo..
     */
    private data class Reg(val instruction: Instruction, var undo: () -> Unit) {
        override fun toString() =
            // Display instruction only, undo cannot be printed in a useful way.
            instruction.toString()
    }

    /**
     * Lock object for synchronization if needed.
     */
    private val sharedLock = Any()

    /**
     * Runs the block for the result applying the desired synchronization.
     */
    private inline fun <R> critical(block: () -> R) =
        if (synchronized)
            synchronized(sharedLock, block)
        else
            block()

    /**
     * Current mode. Changed by exposed methods on standard shell.
     */
    private var sharedMode = Mode.Idle

    /**
     * Save handler.
     */
    private val sharedOnSave = CallbackList()

    /**
     * Resolve handler.
     */
    private val sharedOnAdd = EventList<Lx, Ent>()

    /**
     * Resolve handler.
     */
    private val sharedOnRemove = EventList<Lx, Ent>()

    /**
     * Central entity table for access.
     */
    private val central = HashMap<Lx, Ent>()

    /**
     * Timed instruction register.
     */
    private val register = TreeMap<Time, Reg>()

    /**
     * Local time slots per global.
     */
    private val locals = TreeMap<Long, Byte>()

    /**
     * Active input bundle.
     */
    private var loadFromHandler: (Lx) -> Any? = defaultLoadFromHandler

    /**
     * Active output bundle.
     */
    private var saveToHandler: (id: Lx, value: Any?) -> Unit = defaultSaveToHandler

    /**
     * Currently capturing undo sequence.
     */
    private var currentUndo = hashMapOf<Lx, () -> Unit>()


    override val engine = object : Engine {
        override val shell: Shell
            get() = this@StandardShell

        override val isLoading: Boolean
            get() = sharedMode == Mode.RestoreData

        override val onSave: Callback
            get() = sharedOnSave

        override fun load(id: Lx) =
            loadFromHandler(id)

        override fun save(id: Lx, value: Any?) {
            saveToHandler(id, value)
        }

        override val onAdd: Event<Lx, Ent>
            get() = sharedOnAdd

        override val onRemove: Event<Lx, Ent>
            get() = sharedOnRemove

        override fun add(ent: Ent) {
            critical {
                // Put entity, get existing if present and run block if not null.
                central.put(ent.id, ent)?.let {
                    require(it === ent) {
                        "Cannot add $ent as ${ent.id}, already assigned to $it "
                    }
                }

                // Connect entity.
                ent.driver.connect()

                // Mark resolve from add.
                sharedOnAdd(ent.id, ent)
            }
        }

        override fun remove(ent: Ent) {
            critical {
                // De-resolve from remove.
                sharedOnRemove(ent.id, ent)

                // Disconnect.
                ent.driver.disconnect()

                // Remove entry.
                require(central.remove(ent.id, ent)) {
                    "Cannot remove $ent as ${ent.id}, assigned as ${central[ent.id]} "
                }
            }
        }

        override fun capture(id: Lx, undo: () -> Unit) {
            currentUndo.putIfAbsent(id, undo)
        }

        override fun exchange(instruction: Instruction) {
            critical {
                // Set mode to revoke.
                sharedMode = Mode.Revoke

                // Memorize limit, reset to instruction.
                val before = limit
                if (before > instruction.time)
                    limit = instruction.time

                // Add to register.
                insertToRegister(instruction)

                // Transmit instruction with proxies.
                onTransmit(instruction.toProxyWith(shell))

                // Set mode to invoke.
                sharedMode = Mode.Invoke

                // Reset to previous.
                if (before > instruction.time)
                    limit = before

                // Reset mode.
                sharedMode = Mode.Idle
            }
        }

        override fun local(instructions: Sequence<Instruction>) {
            critical {
                // Create sorted insertion set
                val sorted = TreeMap<Time, Instruction>()
                instructions.associateByTo(sorted) { it.time }

                // Skip if nothing to do.
                if (sorted.isEmpty())
                    return

                // Set mode to revoke.
                sharedMode = Mode.Revoke

                // Memorize limit, reset to instruction.
                val before = limit
                if (before > sorted.firstKey())
                    limit = sorted.firstKey()

                // Add all to register, assert was empty.
                for (instruction in sorted.values)
                    insertToRegister(instruction)

                // Set mode to invoke.
                sharedMode = Mode.Invoke

                // Reset to previous.
                if (before > sorted.firstKey())
                    limit = before

                // Reset mode.
                sharedMode = Mode.Idle
            }
        }

        override fun invalidate(global: Long) {
            critical {
                // Clear the entries leading up to the given time.
                register.headMap(Time(global, Short.MIN_VALUE, Byte.MIN_VALUE)).clear()
                locals.headMap(global).clear()
            }
        }
    }


    /**
     * The player number.
     */
    override val player = validate(player < 0) {
        // TODO: Should in the end not be actually limiting.
        "For standard engine, player IDs are limited to negative section."
    } ?: player


    override var initializedTime = System.currentTimeMillis()
        private set

    override fun time(global: Long): Time {
        critical {
            // Get local time from local scopes.
            val local = locals.compute(global) { _, v ->
                // If value is present, increment it, otherwise increment minimum value.
                v?.inc() ?: Byte.MIN_VALUE.inc()
            }?.dec() ?: never

            // Return time with given values.
            return Time(global, player, local)
        }
    }

    override fun resolve(id: Lx): Ent? {
        critical {
            return central[id]
        }
    }

    override fun <T : Any> list(kClass: KClass<T>): List<T> {
        critical {
            return central.values
                .filter { kClass.isInstance(it) }
                .sortedBy { it.id }
                .map { kClass.cast(it) }
        }
    }


    /**
     * Loads the scope from the bundle.
     */
    fun loadFrom(load: (Lx) -> Any?) {
        critical {
            // Transfer load from handler.
            loadFromHandler = load

            // Reset state for restoring.
            sharedMode = Mode.Revoke
            limit = Time.MIN_VALUE

            // Disconnect and clear entity table.
            sharedMode = Mode.Idle
            central.values.forEach { it.driver.disconnect() }
            central.clear()

            // Clear local time assignments.
            locals.clear()

            // Clear instruction register.
            register.clear()


            // Load initialized time.
            sharedMode = Mode.RestoreData
            initializedTime = load(SIT) as Long

            // Load list of all entities.
            @Suppress("unchecked_cast")
            (load(PET) as List<Lx>).forEach {
                // Get entity class and extra arguments.
                val (c, e) = load(PET / it) as Pair<KClass<Ent>, Map<String, Any?>>

                // Construct by arguments, map extra parameters by their type.
                val ent = c.constructBy(e) { param ->
                    when {
                        param.type.isSupertypeOf(scopeType) -> Box(this)
                        param.type.isSupertypeOf(lxType) -> Box(it)
                        else -> null
                    }
                }

                // Add to central.
                central[it] = ent
            }

            // Connect all restored values.
            central.values.forEach {
                it.driver.connect()
            }

            // Resolve all.
            central.values.forEach {
                sharedOnAdd(it.id, it)
            }

            // Load local per global.
            @Suppress("unchecked_cast")
            (load(LPG / player) as? List<Pair<Long, Byte>>)?.let {
                // Add all entries.
                locals.putAll(it)
            }

            // Load instruction replay table.
            @Suppress("unchecked_cast")
            (load(IRT) as List<Instruction>).let {
                // Associate by time into register.
                it.associateTo(register) { inst ->
                    inst.time to Reg(inst.toValueWith(this)) {}
                }
            }

            // Replay loaded instructions.
            sharedMode = Mode.Invoke
            limit = Time.MAX_VALUE

            // Reset load from handler.
            loadFromHandler = defaultLoadFromHandler

            // Reset state for idle.
            sharedMode = Mode.Idle
        }
    }

    /**
     * Stores the scope to the bundle.
     */
    fun saveTo(save: (id: Lx, value: Any?) -> Unit) {
        critical {
            // Transfer save to handler.
            saveToHandler = save

            // Reset state for storing.
            sharedMode = Mode.Revoke
            limit = Time.MIN_VALUE

            // Store system initialized time.
            sharedMode = Mode.StoreData
            save(SIT, initializedTime)

            // Save all existing IDs.
            central.keys.sorted().let {
                // Save all existing IDs.
                save(PET, it)
            }

            // Save primary entity table.
            central.values.forEach {
                // Save under key and ID, store constructed class and potential extra arguments.
                save(PET / it.id, it::class to it.extraArgs.orEmpty())
            }

            // Save local time slots.
            locals.entries.map { it.key to it.value }.let {
                // Save whole list under local per global.
                save(LPG / player, it)
            }

            // Save instruction register.
            register.values.map { it.instruction }.let {
                // Save whole list under instruction replay table.
                save(IRT, it.map { inst -> inst.toProxyWith(this) })
            }

            // Handle detached saves.
            sharedOnSave()

            // Replay instructions.
            sharedMode = Mode.Invoke
            limit = Time.MAX_VALUE

            // Reset save to handler.
            saveToHandler = defaultSaveToHandler

            // Reset state for idle.
            sharedMode = Mode.Idle
        }
    }

    private var limitValue = Time.MAX_VALUE

    private var limit: Time
        get() = limitValue
        set(value) {
            // Same limit value, operations required.
            if (limitValue == value)
                return

            // Get lower and upper boundary of the invalidation range.
            val lower = minOf(limitValue, value)
            val upper = maxOf(limitValue, value)
            val part = register.subMap(lower, true, upper, false)

            // Check if downward or upward execution.
            if (lower == value) {
                part.descendingMap().values.forEach {
                    it.undo()
                }
            } else {
                part.values.forEach {
                    // Begin undo capture.
                    currentUndo.clear()

                    // Resolve entity, perform operation.
                    resolve(it.instruction.target)?.driver?.perform(it.instruction)

                    // Compile undo.
                    val undos = currentUndo.values.toList()

                    // Assign as new undo.
                    it.undo = {
                        undos.forEach { it() }
                    }
                }
            }

            // Transfer limit value.
            limitValue = value
        }

    /**
     * Outgoing connection node, will be invoked with a fully proxified instruction.
     */
    val onTransmit = HandlerList<Instruction>()

    private fun insertToRegister(instruction: Instruction) {
        val existing = register.put(instruction.time, Reg(instruction) {})
        if (existing != null)
            require(existing.instruction == instruction) {
                "Instruction slot for $instruction occupied by $existing"
            }
    }

    /**
     * Incoming connection node, call with a received proxified instruction.
     */
    fun receive(instructions: Sequence<Instruction>) {
        critical {
            // Create sorted insertion set
            val sorted = TreeMap<Time, Instruction>()
            instructions.associateByTo(sorted) { it.time }

            // Skip if nothing to do.
            if (sorted.isEmpty())
                return

            // Set mode to revoke.
            sharedMode = Mode.Revoke

            // Memorize limit, reset to instruction.
            val before = limit
            if (before > sorted.firstKey())
                limit = sorted.firstKey()

            // Add all to register.
            for (instruction in sorted.values)
                insertToRegister(instruction.toValueWith(this))

            // Set mode to invoke.
            sharedMode = Mode.Invoke

            // Reset to previous.
            if (before > sorted.firstKey())
                limit = before

            // Reset mode.
            sharedMode = Mode.Idle
        }
    }

}

/**
 * Runs the receive method with the var-arg list.
 */
fun StandardShell.receive(vararg instructions: Instruction) =
    receive(instructions.asSequence())