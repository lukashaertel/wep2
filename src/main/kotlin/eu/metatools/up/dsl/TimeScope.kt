package eu.metatools.up.dsl

import eu.metatools.up.aspects.Aspects
import eu.metatools.up.dt.Lx
import eu.metatools.up.dt.Time
import eu.metatools.up.dt.div
import eu.metatools.up.structure.Part
import eu.metatools.up.TPT
import eu.metatools.up.aspects.Store
import eu.metatools.up.aspects.invoke
import eu.metatools.up.lang.autoClosing

/**
 * A time scope generating [Time]s. Allows for functions that take a [Time] as the first argument to automatically be
 * called with a [Time] taken from [take].
 */
abstract class TimeScope {
    /**
     * Takes the next valid time for hte scope.
     */
    abstract fun take(): Time

    /**
     * Invokes the receiver with a fresh time obtained from [take].
     */
    operator fun ((Time) -> Unit).invoke() =
        invoke(take())

    /**
     * Invokes the receiver with a fresh time obtained from [take].
     */
    operator fun <T> ((Time, T) -> Unit).invoke(arg: T) =
        invoke(take(), arg)

    /**
     * Invokes the receiver with a fresh time obtained from [take].
     */
    operator fun <T, U> ((Time, T, U) -> Unit).invoke(arg1: T, arg2: U) =
        invoke(take(), arg1, arg2)

    /**
     * Invokes the receiver with a fresh time obtained from [take].
     */
    operator fun <T, U, V> ((Time, T, U, V) -> Unit).invoke(arg1: T, arg2: U, arg3: V) =
        invoke(take(), arg1, arg2, arg3)

    /**
     * Invokes the receiver with a fresh time obtained from [take].
     */
    operator fun <T, U, V, W> ((Time, T, U, V, W) -> Unit).invoke(arg1: T, arg2: U, arg3: V, arg4: W) =
        invoke(take(), arg1, arg2, arg3, arg4)
}


/**
 * A time source bound to a [player]. Internally memorizes the last generated times to prevent non-strict progression.
 */
class TimeSource(val aspects: Aspects?, val player: Short) : Part {
    override val id: Lx = TPT / player

    /**
     * Close handle for store connection.
     */
    private var closeSave by autoClosing()

    override fun connect() {
        aspects<Store> {
            if (isLoading) {
                lastGlobal = load(id / "global") as Long
                lastInner = load(id / "inner") as Byte
            } else {
                lastGlobal = Long.MIN_VALUE
                lastInner = Byte.MIN_VALUE
            }

            closeSave = handleSave.register {
                it(id / "global", lastGlobal)
                it(id / "inner", lastInner)
            }
        }
    }

    override fun disconnect() {
        closeSave = null
    }

    /**
     * The last global time used.
     */
    private var lastGlobal = Long.MIN_VALUE

    /**
     * The last inner time used for the [lastGlobal] time.
     */
    private var lastInner = Byte.MIN_VALUE

    /**
     * Runs a block with a time scope. Once one [global] time is used, no smaller global time may be used. Passing a
     * smaller time will prevent the [block] to execute and return false.
     */
    inline fun bind(global: Long, block: TimeScope.() -> Unit): Boolean {
        // Assert strict progression, if smaller time was passed, skip invocation.
        @Suppress("non_public_call_from_public_inline")
        if (global < lastGlobal)
            return false

        // If advancing global time, reset the inner time.
        @Suppress("non_public_call_from_public_inline")
        if (global > lastGlobal) {
            lastGlobal = global
            lastInner = Byte.MIN_VALUE
        }

        // Run the block on the time scope.
        @Suppress("non_public_call_from_public_inline")
        block(object : TimeScope() {
            override fun take() =
                Time(global, player, lastInner++)
        })

        // Return true, block passed.
        return true
    }
}
