package eu.metatools.up

import eu.metatools.up.dt.*
import eu.metatools.up.lang.Bind
import eu.metatools.up.notify.*

/**
 * Mode of the scope.
 */
enum class Mode {
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

// TODO Private interface for system functionality.

/**
 * Definition of a scope.
 */
interface Scope {
    /**
     * Current mode.
     */
    val mode: Mode

    /**
     * Converts a value to a proxy.
     */
    fun toProxy(value: Any?): Any?

    /**
     * Converts a proxy to a value.
     */
    fun toValue(proxy: Any?): Any?

    /**
     * Detached handler for saving to [save].
     */
    val onSave: Callback

    /**
     * Loads a value from the field. Usually invoked on connecting an [Ent].
     */
    fun load(id: Lx): Any?

    /**
     * Saves a value to the field. Usually invoked from the [onSave] detached handlers.
     */
    fun save(id: Lx, value: Any?)

    /**
     * Instruction execution limit.
     */
    var limit: Time

    /**
     * Detached handler for instructions on [Ent]s.
     */
    val onPerform: Event<Lx, Instruction>

    /**
     * Captures the reset of the distinct [id] by invoking [undo]. Relative undo, should expect the post-state.
     */
    fun capture(id: Lx, undo: () -> Unit)

    /**
     * Performs the operation, invoke via the [Ent.exchange] wrappers.
     */
    fun exchange(instruction: Instruction)

    /**
     * Performs the operations locally. For [Instruction]s that are manually synchronized.
     */
    fun local(instructions: Sequence<Instruction>)

    /**
     * Includes an [Ent] in the table. Connects it.
     */
    fun include(ent: Ent)

    /**
     * Excludes an [Ent] from the table. Disconnects it.
     */
    fun exclude(id: Lx)

    /**
     * The time the scope was initialized, loaded from store or set on initialize.
     */
    val initializedTime: Long

    /**
     * Provides the unified time for the given global time. Usually [Time.player] is bound by a scope.
     */
    fun time(global: Long): Time

    /**
     * Invalidates every time constrained value before the given time.
     */
    fun invalidate(global: Long)
}

/**
 * Resolves the [Ent]. Delegates to [Scope.toValue] and casts optionally as [Ent].
 */
fun Scope.resolve(id: Lx): Ent? =
    toValue(id) as? Ent

/**
 * Converts the instruction to a proxy instruction via [Scope.toProxy].
 */
fun Instruction.toProxyWith(scope: Scope) =
    Instruction(target, methodName, time, args.map(scope::toProxy))

/**
 * Converts the instruction to a value instruction via [Scope.toValue].
 */
fun Instruction.toValueWith(scope: Scope) =
    Instruction(target, methodName, time, args.map(scope::toValue))


/**
 * Runs the local method with the var-arg list.
 */
fun Scope.local(vararg instructions: Instruction) =
    local(instructions.asSequence())

/**
 * Runs the [block] with a [Bind] on [Scope.time].
 */
inline fun Scope.withTime(global: Long, block: Bind<Time>.() -> Unit) =
    block(Bind(time(global)))

/**
 * Runs the [block] with a [Bind] on [Scope.time].
 */
inline fun Scope.withTime(clock: Clock, block: Bind<Time>.() -> Unit) =
    withTime(clock.time, block)