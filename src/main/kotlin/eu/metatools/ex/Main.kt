package eu.metatools.ex

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.Color
import com.esotericsoftware.minlog.Log
import eu.metatools.ex.data.basicMap
import eu.metatools.ex.ents.*
import eu.metatools.ex.ents.Constants.tileHeight
import eu.metatools.ex.ents.Constants.tileWidth
import eu.metatools.ex.ents.hero.*
import eu.metatools.ex.input.KeyStick
import eu.metatools.ex.input.Look
import eu.metatools.ex.input.Mouse
import eu.metatools.f2d.F2DListener
import eu.metatools.f2d.InOut
import eu.metatools.f2d.data.Mat
import eu.metatools.f2d.data.QPt
import eu.metatools.f2d.data.Vec
import eu.metatools.f2d.data.toQ
import eu.metatools.f2d.drawable.Drawable
import eu.metatools.f2d.drawable.tint
import eu.metatools.up.*
import eu.metatools.up.dt.Instruction
import eu.metatools.up.dt.Lx
import eu.metatools.up.dt.div
import eu.metatools.up.dt.lx
import eu.metatools.up.net.NetworkClaimer
import eu.metatools.up.net.NetworkClock
import eu.metatools.up.net.NetworkSignOff
import eu.metatools.up.net.makeNetwork
import java.util.*
import kotlin.NoSuchElementException
import kotlin.reflect.KParameter
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.isSupertypeOf
import kotlin.reflect.typeOf

/**
 * True if state hashes should be displayed.
 */
private var debug = false

/**
 * True if network should be debugged.
 */
private var debugNet = false

/**
 * Shell hash receiver.
 */
private val shellHashes = ShellHashes()

/**
 * Seconds for a long millis time stamp.
 */
val Long.sec get() = this / 1000.0

/**
 * Gets the next full second.
 */
fun Long.toNextFullSecond() =
    1000L - (this % 1000L)

/**
 * Z-value for sub-UI.
 */
const val subUiZ = -40f
/**
 * Z-value for UI.
 */
const val uiZ = -50f

/**
 * Draws two texts so it looks like they have a shadow.
 */
fun InOut.shadowText(on: Drawable<String>, text: String, time: Double, x: Float, y: Float, size: Float, d: Float = 2f) {
    submit(on.tint(Color.BLACK), text, time, Mat.translation(x + d, y - d, uiZ).scale(size, size))
    submit(on, text, time, Mat.translation(x, y, uiZ).scale(size, size))
}

class Frontend : F2DListener(-100f, 100f) {
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
     * Network connection.
     */
    private val net = makeNetwork("next-cluster", { handleBundle() }, { handleReceive(it) },
        configureKryo = ::configureKryo
    )

    /**
     * Network clock.
     */
    private val clock = NetworkClock(net)

    /**
     * Network player claimer. Claims and holds a player ID per UUID.
     */
    private val claimer = NetworkClaimer(net, UUID.randomUUID(), changed = { old, new ->
        System.err.println("Warning: Claim for engine has changed from $old to $new, this should not happen.")
    })

    /**
     * Sign-off coordinator.
     */
    private val signOff = NetworkSignOff(net,
        initialDelay = clock.time.toNextFullSecond(),
        changed = { _, new ->
            if (new != null)
                signOffValue = new
        })

    /**
     * The shell that runs the game.
     */
    val shell = StandardShell(claimer.currentClaim).also {
        it.send = net::instruction
    }

    /**
     * Resolves global parameter for entities. Used to provide the UI references.
     */
    @Suppress("experimental_api_usage_error")
    private fun resolveGlobal(param: KParameter): Any {
        // If F2DListener <= type <= Frontend, return this.
        if (param.type.isSubtypeOf(typeOf<F2DListener>()) && param.type.isSupertypeOf(typeOf<Frontend>()))
            return this

        // Unknown.
        throw NoSuchElementException(param.toString())
    }

    /**
     * The current time of the connected system.
     */
    override val time: Double
        get() = (clock.time - shell.initializedTime).sec


    /**
     * Root world.
     */
    lateinit var root: World

    /**
     * Sign off value, received by the sign off coordinator.
     */
    private var signOffValue: Long? = null

    override fun create() {
        super.create()

        // Set model scaling to display scaled up.
        view = Mat.scaling(scaling)

        // Set title.
        Gdx.graphics.setTitle("Joined, player: ${shell.player}")

        // Critically execute.
        shell.critical {
            // Assign world from loading or creating.
            root = if (net.isCoordinating) {
                // This instance is coordinating, create the world.
                World(shell, lx / "root", this, basicMap).also {
                    shell.engine.add(it)
                }
            } else {
                // Joined, restore and resolve root.
                val bundle = net.bundle()
                shell.loadFromMap(bundle, ::resolveGlobal, check = true)
                shell.resolve(lx / "root") as World
            }

            // On joining, create a mover.
            shell.withTime(clock) {
                root.createHero(shell.player)
            }
        }
    }

    /**
     * Returns the look direction.
     */
    private val centerLook = Look()

    /**
     * Turns key input into (x, y) coordinates.
     */
    private val keyStick = KeyStick()

    /**
     * Provides extended mouse functions.
     */
    private val mouse = Mouse()

    /**
     * Gets the own mover.
     */
    private fun ownMover(): Hero? =
        shell.list<Hero>().find { it.owner == shell.player }

    override fun render() {
        // Block network on all rendering, including sending via Once.
        shell.critical {
            super.render()
        }
    }

    /**
     * Current scaling factor.
     */
    private var scaling = 4f

    override fun render(time: Double, delta: Double) {
        // Bind current time.
        shell.withTime(clock) {
            // Get own mover (might be non-existent).
            val hero = ownMover()

            // Check if mover is there.
            if (hero == null) {
                // Set to scaling.
                view = Mat.scaling(scaling)

                // If key just pressed, create a new mover.
                if (Gdx.input.isKeyJustPressed(Keys.F1))
                    root.createHero(shell.player)
            } else {
                // Get the coordinate of the mover.
                val (x, y, z) = hero.xyz(time)

                // Center on it.
                view = Mat
                    .translation(Gdx.graphics.width / 2f, Gdx.graphics.height / 2f)
                    .scale(scaling,scaling)
                    .translate(x = -x.toFloat() * tileWidth, y = -y.toFloat() * tileHeight)
                    .translate(y = -z.toFloat() * tileHeight)

                // Get desired move direction.
                keyStick.fetch()?.let {
                    // Send to mover if present.
                    hero.move(it.toQ())
                }

                // If look at direction has changed, look at it.
                centerLook.fetch()?.let {
                    hero.lookAt(it)
                }

                // If mouse status has changed, handle input.
                mouse.fetch()?.let {
                    if (mouse.justPressed(Input.Buttons.RIGHT))
                        hero.cancelDraw()

                    if (mouse.justPressed(Input.Buttons.LEFT))
                        hero.draw()

                    if (mouse.justReleased(Input.Buttons.LEFT))
                        hero.release(QPt(mouse.dx, mouse.dy))
                }

                // Render mover parameters.
                ui.submitHealth(hero, time)
                ui.submitDraw(hero, time)
                ui.submitAmmo(hero, time)
                ui.submitXP(hero, time)
            }
        }

        // Submit described elements.
        ui.submitDescribed(capture?.first as? Described, time)

        // Dispatch global update.
        root.worldUpdate(clock.time)

        // Render everything.
        shell.list<Rendered>().forEach { it.render(Mat.ID, time) }

        // Check if sign off was set.
        signOffValue?.let {
            // Invalidate to it and reset sign off.
            shell.engine.invalidate(it)
            signOffValue = null

            // Update hash values if debugging.
            if (debug) shellHashes.pushHash(shell) else shellHashes.clear()
        }

        // Draw ping.
        ui.submitPing(clock, time)

        // Display all hashes if debugging.
        if (debug) ui.submitHashes(shellHashes, time)

        // Handle other inputs.
        otherInputs()
    }

    /**
     * Handles non-game stuff.
     */
    private fun otherInputs() {
        // Set zoom.
        if (Gdx.input.isKeyJustPressed(Keys.NUM_1))
            scaling = 1f
        if (Gdx.input.isKeyJustPressed(Keys.NUM_2))
            scaling = 2f
        if (Gdx.input.isKeyJustPressed(Keys.NUM_3))
            scaling = 3f
        if (Gdx.input.isKeyJustPressed(Keys.NUM_4))
            scaling = 4f
        if (Gdx.input.isKeyJustPressed(Keys.NUM_5))
            scaling = 5f

        // Toggle debug.
        if (Gdx.input.isKeyJustPressed(Keys.F9))
            debug = !debug

        // Toggle network debug.
        if (Gdx.input.isKeyJustPressed(Keys.F10)) {
            debugNet = !debugNet
            Log.set(if (debugNet) Log.LEVEL_DEBUG else Log.LEVEL_INFO)
        }
    }

    /**
     * Current captured element and position.
     */
    var capture: Pair<Any, QPt>? = null
        private set

    /**
     * True if [any] is the first entry of [capture].
     */
    fun isSelected(any: Any) =
        capture?.first == any

    override fun capture(result: Any?, intersection: Vec) {
        // Get actual coordinates.
        val (x, y) = view.inv * intersection

        // Memorize result, convert to world space.
        capture = (result ?: root) to QPt(x / tileWidth, y / tileHeight)
    }

    override fun pause() = Unit

    override fun resume() = Unit

    override fun dispose() {
        super.dispose()

        signOff.close()
        claimer.close()
        clock.close()
        net.close()
    }
}

/**
 * Frontend instance.
 */
lateinit var frontend: Frontend

fun main() {
    // Create the frontend.
    frontend = Frontend()

    // Set config values.
    val config = LwjglApplicationConfiguration()
    config.height = config.width

    // Start application.
    LwjglApplication(frontend, config)
}