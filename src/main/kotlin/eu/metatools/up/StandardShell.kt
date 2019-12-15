package eu.metatools.up

import eu.metatools.up.dt.*
import eu.metatools.up.lang.constructBy
import eu.metatools.up.lang.never
import java.util.*
import kotlin.collections.HashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.cast
import kotlin.reflect.full.isSupertypeOf
import kotlin.reflect.typeOf

/**
 * Standard shell and engine with bound player and proxy nodes for incoming and outgoing instructions.
 */
class StandardShell(override val player: Short, val synchronized: Boolean = true) : Shell {
    companion object {
        /**
         * Type of the [Shell], used for restore.
         */
        @Suppress("experimental_api_usage_error")
        private val scopeType = typeOf<Shell>()

        /**
         * Type of the [Lx], used for restore.
         */
        @Suppress("experimental_api_usage_error")
        private val lxType = typeOf<Lx>()

        /**
         * Bundle root.
         */
        private val bundleRoot = lx / ".engine"

        /**
         * Bundle key for [Shell] relative save data initialized time.
         */
        private const val nameInitTime = ".init-time"

        /**
         * Bundle key for [Shell] relative save data entities in central table.
         */
        private const val nameEntityIDs = ".entity-ids"

        /**
         * Bundle key for [Shell]/[Ent] relative save data constructor bundle.
         */
        private const val nameConstructor = ".constructor"

        /**
         * Bundle key for [Shell] relative save data instruction register.
         */
        private const val nameRegister = ".register"

        /**
         * Susudio.
         */
        val genesis = Time.MIN_VALUE
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
    inline fun <R> critical(block: () -> R) =
        if (synchronized)
            @Suppress("non_public_call_from_public_inline")
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
    private var currentUndo = mutableListOf<() -> Unit>()


    /**
     * Run the [block] with the status at time. Will revert every instruction past the [time] and after invoking
     * the [block] redo every registered instruction. Always runs [critical].
     */
    private inline fun runWithStateOf(time: Time, block: () -> Unit) {
        critical {
            val part = register.tailMap(time, true)

            // Descending undo.
            part.descendingMap().values.forEach {
                it.undo()
            }

            // Run block.
            block()

            // Ascending redo and overwrite.
            part.values.forEach {
                // Begin undo capture.
                currentUndo.clear()

                // Resolve entity, perform operation.
                resolve(it.instruction.target)?.driver?.perform(it.instruction)

                // Compile undo.
                val undos = currentUndo.asReversed().toList()

                // Assign as new undo.
                it.undo = {
                    undos.forEach { it() }
                }
            }
        }
    }

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

                // Connect entity, mark as ready.
                ent.driver.connect(null)
                ent.driver.ready()
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

        override fun capture(undo: () -> Unit) {
            currentUndo.add(undo)
        }

        override fun exchange(instruction: Instruction) {
            runWithStateOf(instruction.time) {
                // Add to register, transmit instruction with proxies.
                insertToRegister(instruction)
                send?.invoke(instruction.toProxyWith(shell))
            }
        }

        override fun local(instructions: Sequence<Instruction>) {
            // Get minimum revert time.
            val limit = instructions.map(Instruction::time).min() ?: return

            // Run in that time.
            runWithStateOf(limit) {
                // Add all to register, assert was empty.
                for (instruction in instructions)
                    insertToRegister(instruction)
            }
        }

        override fun invalidate(global: Long) {
            critical {
                // Clear the entries leading up to the given time.
                register.headMap(Time(global, Short.MIN_VALUE, Byte.MIN_VALUE)).clear()
                locals.headMap(global).clear()
            }
        }

        override fun lastRepeatingTime(player: Short, rdc: Byte): Long? {
            critical {
                return register.keys
                    .asSequence()
                    .filter { it.player == player && it.local == rdc }
                    .map { it.global }
                    .min()
            }
        }
    }

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

    override fun <T : Any> list(kClass: KClass<T>): Sequence<T> {
        critical {
            // Provide all entities that are instance of T, sorted by the ID. Then turn to sequence and filter contained
            // in the entity table (sequence might change on iteration).
            return central.values
                .filter { kClass.isInstance(it) }
                .sortedBy { it.id }
                .asSequence()
                .filter { it in central.values }
                .map { kClass.cast(it) }
        }
    }

    override fun load(shellIn: ShellIn) {
        runWithStateOf(genesis) {
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

            // Load instruction replay table.
            @Suppress("unchecked_cast")
            (shellIn(bundleRoot / nameRegister) as List<Instruction>).let {
                // Associate by time into register.
                it.associateTo(register) { inst ->
                    inst.time to Reg(inst.toValueWith(this)) {}
                }
            }

            // Restore local times, the local table must contain the next valid local ID, this grouping satisfies this.
            register.keys
                .asSequence()
                .filter { it.player == player }
                .groupingBy { it.global }
                .fold(Byte.MIN_VALUE) { p, c -> maxOf(p, c.local) }
                .mapValuesTo(locals) { (_, v) -> v.inc() }

            // Ready all values.
            central.values.forEach {
                it.driver.ready()
            }
        }
    }

    override fun store(shellOut: ShellOut) {
        runWithStateOf(genesis) {
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

            // Save instruction register.
            register.values.map { it.instruction }.let {
                // Save whole list under instruction replay table.
                shellOut(bundleRoot / nameRegister, it.map { inst -> inst.toProxyWith(this) })
            }
        }
    }

    /**
     * Outgoing connection node, will be invoked with a fully proxified instruction.
     */
    override var send: ((Instruction) -> Unit)? = null

    /**
     * Incoming connection node, call with a received proxified instruction.
     */
    override fun receive(instructions: Sequence<Instruction>) {
        // Get minimum revert time.
        val limit = instructions.map(Instruction::time).min() ?: return

        runWithStateOf(limit) {
            // Add all to register from proxies, assert was empty.
            for (instruction in instructions)
                insertToRegister(instruction.toValueWith(this))
        }
    }

    private fun insertToRegister(instruction: Instruction) {
        val before = register.put(instruction.time, Reg(instruction) {})
        require(before == null || before.instruction == instruction) {
            "Instruction slot ${instruction.time} already occupied."
        }
    }
}