package eu.metatools.ex

import com.esotericsoftware.kryo.Kryo
import eu.metatools.fio.FioListener
import eu.metatools.up.StandardShell
import eu.metatools.up.dt.Instruction
import eu.metatools.up.dt.Lx
import eu.metatools.up.dt.Time
import eu.metatools.up.lang.Bind
import eu.metatools.up.loadFromMap
import eu.metatools.up.net.NetworkClaimer
import eu.metatools.up.net.NetworkClock
import eu.metatools.up.net.NetworkSignOff
import eu.metatools.up.net.makeNetwork
import eu.metatools.up.receive
import eu.metatools.up.withTime
import java.util.*
import kotlin.NoSuchElementException
import kotlin.reflect.KParameter
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.typeOf

/**
 * Basic game infrasturcture.
 * @param near Near plane of the [FioListener], less than [far].
 * @param far Far plane of the [FioListener], greater than [near].
 * @param machineID The machine ID or empty, if no locally unique player ID is needed.
 */
abstract class BaseGame(near: Float, far: Float, machineID: UUID = UUID.randomUUID()) : FioListener(near, far) {
    /**
     * Network connection.
     */
    protected val net = makeNetwork("next-cluster",
        onBundle = { handleBundle() },
        onReceive = { handleReceive(it) },
        configureKryo = { configureNet(it) }
    )

    /**
     * Network clock.
     */
    protected val netClock = NetworkClock(net)

    /**
     * Network player claimer. Claims and holds a player ID per UUID.
     */
    protected val netClaimer = NetworkClaimer(net, machineID, changed = { old, new ->
        System.err.println("Warning: Claim for engine has changed from $old to $new, this should not happen.")
    })

    /**
     * Sign-off coordinator.
     */
    protected val netSignOff = NetworkSignOff(net,
        initialDelay = netClock.time.toNextFullSecond(),
        changed = { _, new ->
            if (new != null)
                signOffValue = new
        })

    /**
     * The shell that runs the game.
     */
    val shell = StandardShell(netClaimer.currentClaim).also {
        it.send = net::instruction
    }

    /**
     * The current time of the connected system.
     */
    override val time: Double
        get() = (netClock.time - shell.initializedTime).sec

    /**
     * Sign off value, received by the sign off coordinator.
     */
    private var signOffValue: Long? = null

    /**
     * Handle bundling the engine.
     */
    private fun handleBundle(): Map<Lx, Any?> {
        val result = hashMapOf<Lx, Any?>()
        shell.store(result::set)
        return result
    }

    /**
     * Handle receiving the instruction.
     */
    private fun handleReceive(instruction: Instruction) {
        shell.receive(instruction)
    }

    /**
     * Configures network serialization.
     */
    protected abstract fun configureNet(kryo: Kryo)

    /**
     * Resolves global parameter for entities.
     */
    @Suppress("experimental_api_usage_error")
    private fun resolveGlobal(param: KParameter): Any {
        // If assignable to interface or class, return this instance.
        if (param.type.isSubtypeOf(typeOf<FioListener>())) return this
        if (param.type.isSubtypeOf(typeOf<BaseGame>())) return this

        // Unknown.
        throw NoSuchElementException(param.toString())
    }

    override fun create() {
        super.create()

        // Initialize output of instance.
        outputInit()

        // Critically execute.
        shell.critical {
            // Assign world from loading or creating.
            if (net.isCoordinating) {
                // This instance is coordinating, create critical entities.
                shellCreate()
            } else {
                // Joined, restore.
                val bundle = net.bundle()
                shell.loadFromMap(bundle, ::resolveGlobal, check = true)

                // Resolve critical entities.
                shellResolve()
            }

            // On joining, create a mover.
            shell.withTime(netClock) {
                shellAlways()
            }
        }
    }

    /**
     * Run before network is connected and after engine resources may be used.
     */
    protected abstract fun outputInit()

    /**
     * Resolves critical entities of a game on joining (e.g., root world entity).
     */
    protected abstract fun shellResolve()

    /**
     * Creates the critical entities of a game on creation (e.g., root world entity).
     */
    protected abstract fun shellCreate()

    /**
     * Run after shell was created or resolved. Runs with the synchronized time at that point.
     */
    protected abstract fun Bind<Time>.shellAlways()

    /**
     * Critically renders the [FioListener]s [FioListener.render] method.
     */
    final override fun render() {
        // Block network on all rendering, including sending via Once.
        shell.critical {
            super.render()
        }
    }

    /**
     * Creates for a render step the inputs to the shell from the user.
     */
    protected abstract fun Bind<Time>.inputShell(time: Double, delta: Double)

    /**
     * After processing inputs to shell, should update repeating calls.
     */
    protected abstract fun inputRepeating(timeMs: Long)

    /**
     * Renders and updates the game.
     */
    final override fun render(time: Double, delta: Double) {
        // Bind current time.
        shell.withTime(netClock) {
            // Use in input-to-shell generator.
            inputShell(time, delta)
        }

        // Use current time's long value for repeating.
        inputRepeating(netClock.time)

        // Handle other inputs.
        inputOther(time, delta)

        // Check if sign off was set.
        signOffValue?.let {
            shell.engine.invalidate(it)
            signOffValue = null
            // Invalidate to it and reset sign off.
            signOff(it)
        }

        // Output shell relevant data.
        outputShell(time, delta)

        // Output rest. (TODO: Not really necessary, same phase).
        outputOther(time, delta)
    }

    /**
     * Called after shell lower boundary times were exchanged and applied. The shell is at the most reduced state
     * possible.
     */
    protected open fun signOff(it: Long) = Unit

    /**
     * Generates the output of the shell, i.e., how entities are displayed.
     */
    protected abstract fun outputShell(time: Double, delta: Double)

    /**
     * Generates extra output. This method might be merged with [outputShell].
     */
    protected abstract fun outputOther(time: Double, delta: Double)


    /**
     * Handles non-shell-critical input, i.e., UI movement.
     */
    protected abstract fun inputOther(time: Double, delta: Double)

    /**
     * Does nothing on pause.
     */
    override fun pause() = Unit

    /**
     * Does nothing on resume.
     */
    override fun resume() = Unit

    /**
     * Disposes of the network resources.
     */
    override fun dispose() {
        // Close network components and network itself.
        netSignOff.close()
        netClaimer.close()
        netClock.close()
        net.close()

        // Dispose super.
        super.dispose()
    }
}