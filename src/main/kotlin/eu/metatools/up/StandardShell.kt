package eu.metatools.up

import eu.metatools.up.dt.*
import eu.metatools.up.lang.constructBy
import eu.metatools.up.lang.never
import eu.metatools.up.lang.validate
import java.util.*
import kotlin.collections.HashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.cast
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSupertypeOf

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

        private val bundleRoot = lx / ".engine"

        private const val nameInitTime = ".init-time"
        private const val nameEntityIDs = ".entity-ids"
        private const val nameConstructor = ".constructor"
        private const val nameRegister = ".register"
        private const val nameTimeLocals = ".time-locals"

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
     * Currently capturing undo sequence.
     */
    private var currentUndo = hashMapOf<Lx, () -> Unit>()


    override val engine = object : Engine {
        override val shell: Shell
            get() = this@StandardShell

        override fun add(ent: Ent) {
            critical {
                // Put entity, get existing if present and run block if not null.
                central.put(ent.id, ent)?.let {
                    require(it === ent) {
                        "Cannot add $ent as ${ent.id}, already assigned to $it "
                    }
                }

                // Connect entity.
                ent.driver.connect(null)
            }
        }

        override fun remove(ent: Ent) {
            critical {
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
                // Memorize limit, reset to instruction.
                val before = limit
                if (before > instruction.time)
                    limit = instruction.time

                // Add to register.
                insertToRegister(instruction)

                // Transmit instruction with proxies.
                onTransmit?.invoke(instruction.toProxyWith(shell))

                // Reset to previous.
                if (before > instruction.time)
                    limit = before
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

                // Memorize limit, reset to instruction.
                val before = limit
                if (before > sorted.firstKey())
                    limit = sorted.firstKey()

                // Add all to register, assert was empty.
                for (instruction in sorted.values)
                    insertToRegister(instruction)

                // Reset to previous.
                if (before > sorted.firstKey())
                    limit = before
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

    override fun load(shellIn: ShellIn) {
        critical {
            // Reset state for restoring.
            limit = Time.MIN_VALUE

            // Disconnect and clear entity table.
            central.values.forEach { it.driver.disconnect() }
            central.clear()

            // Clear local time assignments.
            locals.clear()

            // Clear instruction register.
            register.clear()


            // Load initialized time.
            initializedTime = shellIn(bundleRoot / nameInitTime) as Long

            // Load list of all entities.
            @Suppress("unchecked_cast")
            (shellIn(bundleRoot / nameEntityIDs) as List<Lx>).forEach {
                // Get entity class and extra arguments.
                val (c, e) = shellIn(bundleRoot / it / nameConstructor) as Pair<KClass<Ent>, Map<String, Any?>>

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
                it.driver.connect { name, key -> shellIn(it.id / name / key) }
            }

            // Load local per global.
            @Suppress("unchecked_cast")
            (shellIn(bundleRoot / player / nameTimeLocals) as? List<Pair<Long, Byte>>)?.let {
                // Add all entries.
                locals.putAll(it)
            }

            // Load instruction replay table.
            @Suppress("unchecked_cast")
            (shellIn(bundleRoot / nameRegister) as List<Instruction>).let {
                // Associate by time into register.
                it.associateTo(register) { inst ->
                    inst.time to Reg(inst.toValueWith(this)) {}
                }
            }

            // Replay loaded instructions.
            limit = Time.MAX_VALUE
        }
    }

    override fun store(shellOut: ShellOut) {
        critical {
            // Reset state for storing.
            limit = Time.MIN_VALUE

            // Store system initialized time.
            shellOut(bundleRoot / nameInitTime, initializedTime)

            // Save all existing IDs.
            central.keys.sorted().let {
                // Save all existing IDs.
                shellOut(bundleRoot / nameEntityIDs, it)
            }

            // Save primary entity table.
            central.values.forEach {
                // Save under key and ID, store constructed class and potential extra arguments.
                shellOut(bundleRoot / it.id / nameConstructor, it::class to it.extraArgs.orEmpty())

                // Save entity parts.
                it.driver.persist { name, key, value -> shellOut(it.id / name / key, value) }
            }

            // Save local time slots.
            locals.entries.map { it.key to it.value }.let {
                // Save whole list under local per global.
                shellOut(bundleRoot / player / nameTimeLocals, it)
            }

            // Save instruction register.
            register.values.map { it.instruction }.let {
                // Save whole list under instruction replay table.
                shellOut(bundleRoot / nameRegister, it.map { inst -> inst.toProxyWith(this) })
            }

            // Replay instructions.
            limit = Time.MAX_VALUE
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
    var onTransmit: ((Instruction) -> Unit)? = null

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

            // Memorize limit, reset to instruction.
            val before = limit
            if (before > sorted.firstKey())
                limit = sorted.firstKey()

            // Add all to register.
            for (instruction in sorted.values)
                insertToRegister(instruction.toValueWith(this))

            // Reset to previous.
            if (before > sorted.firstKey())
                limit = before
        }
    }

}

/**
 * Runs the receive method with the var-arg list.
 */
fun StandardShell.receive(vararg instructions: Instruction) =
    receive(instructions.asSequence())