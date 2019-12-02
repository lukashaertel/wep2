package eu.metatools.up

import eu.metatools.up.dt.*
import eu.metatools.up.lang.constructBy
import eu.metatools.up.lang.never
import eu.metatools.up.notify.CallbackList
import eu.metatools.up.notify.EventList
import eu.metatools.up.notify.HandlerList
import java.util.*
import kotlin.collections.HashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSupertypeOf
import kotlin.reflect.full.safeCast

/**
 * Standard shell and engine with bound player and proxy nodes for incoming and outgoing instructions.
 */
class StandardEngine(val player: Short) : Engine {
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
        private  val PET = lx / engineDomain / "PET"

        /**
         * Local time per global.
         */
        private val LPG = lx / engineDomain / "LPG"

        /**
         * Instruction replay table.
         */
        private  val IRT = lx / engineDomain / "IRT"
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
     * Central entity table for access.
     */
    private val central =
        HashMap<Lx, Ent>()

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
    private lateinit var loadFromHandler: (Lx) -> Any?

    /**
     * Active output bundle.
     */
    private lateinit var saveToHandler: (id: Lx, value: Any?) -> Unit

    /**
     * Currently capturing undo sequence.
     */
    private var currentUndo = hashMapOf<Lx, () -> Unit>()

    /**
     * Loads the scope from the bundle.
     */
    fun loadFrom(load: (Lx) -> Any?) {
        loadFromHandler = load

        // Reset state for restoring.
        mode = Mode.Revoke
        limit = Time.MIN_VALUE

        // Disconnect and clear entity table.
        mode = Mode.Idle
        central.values.forEach { it.driver.disconnect() }
        central.clear()

        // Clear local time assignments.
        locals.clear()

        // Clear instruction register.
        register.clear()


        // Load initialized time.
        mode = Mode.RestoreData
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
        central.values.forEach { it.driver.connect() }

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
        mode = Mode.Invoke
        limit = Time.MAX_VALUE

        // Reset state for idle.
        mode = Mode.Idle
    }

    /**
     * Stores the scope to the bundle.
     */
    fun saveTo(save: (id: Lx, value: Any?) -> Unit) {
        saveToHandler = save

        // Reset state for storing.
        mode = Mode.Revoke
        limit = Time.MIN_VALUE

        // Store system initialized time.
        mode = Mode.StoreData
        save(SIT, initializedTime)

        // Save all existing IDs.
        central.keys.toList().let {
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
        onSave()

        // Replay instructions.
        mode = Mode.Invoke
        limit = Time.MAX_VALUE

        // Reset state for idle.
        mode = Mode.Idle
    }

    override val engine: Engine
        get() = this

    override var mode: Mode = Mode.Idle
        private set

    override fun toProxy(value: Any?): Any? =
        when (value) {
            // Resolve identified object to it's Lx.
            is Ent -> value.id

            // Recursively apply to list elements.
            is List<*> -> value.mapTo(arrayListOf()) {
                toProxy(it)
            }

            // Recursively apply to array elements.
            is Array<*> -> Array(value.size) {
                toProxy(value[it])
            }

            // Recursively apply to set entries.
            is Set<*> -> value.mapTo(mutableSetOf()) {
                toProxy(it)
            }

            // Recursively apply to map entries.
            is Map<*, *> -> value.entries.associateTo(mutableMapOf()) {
                toProxy(it.key) to toProxy(it.value)
            }

            // Recursively apply to triple entries.
            is Triple<*, *, *> -> Triple(toProxy(value.first), toProxy(value.second), toProxy(value.third))

            // Recursively apply to pair entries.
            is Pair<*, *> -> Pair(toProxy(value.first), toProxy(value.second))

            // Return just the value.
            else -> value
        }

    override fun toValue(proxy: Any?): Any? =
        when (proxy) {
            // Resolve Lx to identified object.
            is Lx -> central[proxy]

            // Recursively apply to list elements.
            is List<*> -> proxy.mapTo(arrayListOf()) {
                toValue(it)
            }

            // Recursively apply to array elements.
            is Array<*> -> Array(proxy.size) {
                toValue(proxy[it])
            }

            // Recursively apply to set entries.
            is Set<*> -> proxy.mapTo(mutableSetOf()) {
                toValue(it)
            }

            // Recursively apply to map entries.
            is Map<*, *> -> proxy.entries.associateTo(mutableMapOf()) {
                toValue(it.key) to toValue(it.value)
            }

            // Recursively apply to triple entries.
            is Triple<*, *, *> -> Triple(toValue(proxy.first), toValue(proxy.second), toValue(proxy.third))

            // Recursively apply to pair entries.
            is Pair<*, *> -> Pair(toValue(proxy.first), toValue(proxy.second))

            // Return just the value.
            else -> proxy
        }

    override val onSave = CallbackList()

    override fun load(id: Lx) =
        loadFromHandler(id)

    override fun save(id: Lx, value: Any?) {
        saveToHandler(id, value)
    }

    private var limitValue = Time.MAX_VALUE

    override var limit: Time
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

                    // Perform detached.
                    onPerform(it.instruction.target, it.instruction)

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

    override val onPerform =
        EventList<Lx, Instruction>()

    override fun capture(id: Lx, undo: () -> Unit) {
        currentUndo.putIfAbsent(id, undo)
    }

    /**
     * Outgoing connection node, will be invoked with a fully proxified instruction.
     */
    val onTransmit =
        HandlerList<Instruction>()

    /**
     * Incoming connection node, call with a received proxified instruction.
     */
    fun receive(instructions: Sequence<Instruction>) {
        // Create sorted insertion set
        val sorted = TreeMap<Time, Instruction>()
        instructions.associateByTo(sorted) { it.time }

        // Skip if nothing to do.
        if (sorted.isEmpty())
            return

        // Set mode to revoke.
        mode = Mode.Revoke

        // Memorize limit, reset to instruction.
        val before = limit
        if (before > sorted.firstKey())
            limit = sorted.firstKey()

        // Add all to register, assert was empty.
        for (instruction in sorted.values)
            require(register.put(instruction.time, Reg(instruction.toValueWith(this)) {}) == null) {
                "Instruction slot for $instruction occupied"
            }

        // Set mode to invoke.
        mode = Mode.Invoke

        // Reset to previous.
        if (before > sorted.firstKey())
            limit = before

        // Reset mode.
        mode = Mode.Idle
    }

    override fun exchange(instruction: Instruction) {
        // Set mode to revoke.
        mode = Mode.Revoke

        // Memorize limit, reset to instruction.
        val before = limit
        if (before > instruction.time)
            limit = instruction.time

        // Add to register, assert was empty.
        require(register.put(instruction.time, Reg(instruction) {}) == null) {
            "Instruction slot for $instruction occupied"
        }

        // Transmit instruction with proxies.
        onTransmit(instruction.toProxyWith(this))

        // Set mode to invoke.
        mode = Mode.Invoke

        // Reset to previous.
        if (before > instruction.time)
            limit = before

        // Reset mode.
        mode = Mode.Idle
    }

    override fun local(instructions: Sequence<Instruction>) {
        // Create sorted insertion set
        val sorted = TreeMap<Time, Instruction>()
        instructions.associateByTo(sorted) { it.time }

        // Skip if nothing to do.
        if (sorted.isEmpty())
            return

        // Set mode to revoke.
        mode = Mode.Revoke

        // Memorize limit, reset to instruction.
        val before = limit
        if (before > sorted.firstKey())
            limit = sorted.firstKey()

        // Add all to register, assert was empty.
        for (instruction in sorted.values)
            require(register.put(instruction.time, Reg(instruction) {}) == null) {
                "Instruction slot for $instruction occupied"
            }

        // Set mode to invoke.
        mode = Mode.Invoke

        // Reset to previous.
        if (before > sorted.firstKey())
            limit = before

        // Reset mode.
        mode = Mode.Idle
    }

    override fun add(ent: Ent) {
        // Put entity, get existing.
        val existing = central.put(ent.id, ent)

        // Assert no existing assignment.
        require(existing == null) { "Cannot include $ent as ${ent.id}, already assigned to $existing " }

        // Connect entity.
        ent.driver.connect()
    }

    override fun remove(id: Lx) {
        central.compute(id) { k, v ->
            // Assert value is present.
            require(v != null) { "Cannot exclude $k, not assigned." }

            // Disconnect.
            v.driver.disconnect()

            // Remove.
            null
        }
    }

    override fun resolve(id: Lx) =
        central[id]

    override fun <T : Ent> list(kClass: KClass<T>) =
        central.values.mapNotNull { kClass.safeCast(it) }.sortedBy { it.id }


    override var initializedTime = System.currentTimeMillis()
        private set

    override fun time(global: Long): Time {
        // Get local time from local scopes.
        val local = locals.compute(global) { _, v ->
            // If value is present, increment it, otherwise increment minimum value.
            v?.inc() ?: Byte.MIN_VALUE.inc()
        }?.dec() ?: never

        // Return time with given values.
        return Time(global, player, local)
    }

    override fun invalidate(global: Long) {
        // Clear the entries leading up to the given time.
        register.headMap(Time(global, Short.MIN_VALUE, Byte.MIN_VALUE)).clear()
        locals.headMap(global).clear()
    }
}

/**
 * Runs the receive method with the var-arg list.
 */
fun StandardEngine.receive(vararg instructions: Instruction) =
    receive(instructions.asSequence())