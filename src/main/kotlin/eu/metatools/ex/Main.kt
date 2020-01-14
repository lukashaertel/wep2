package eu.metatools.ex

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.Color
import com.esotericsoftware.minlog.Log
import com.google.common.hash.Hashing
import eu.metatools.ex.data.Dir
import eu.metatools.ex.data.basicMap
import eu.metatools.ex.ents.Constants.tileHeight
import eu.metatools.ex.ents.Constants.tileWidth
import eu.metatools.ex.ents.Described
import eu.metatools.ex.ents.Rendered
import eu.metatools.ex.ents.World
import eu.metatools.ex.ents.hero.*
import eu.metatools.ex.ents.xyz
import eu.metatools.ex.input.KeyStick
import eu.metatools.f2d.F2DListener
import eu.metatools.f2d.InOut
import eu.metatools.f2d.data.Mat
import eu.metatools.f2d.data.QPt
import eu.metatools.f2d.data.Vec
import eu.metatools.f2d.data.toQ
import eu.metatools.f2d.drawable.Drawable
import eu.metatools.f2d.drawable.tint
import eu.metatools.f2d.immediate.submit
import eu.metatools.f2d.resource.LifecycleDrawable
import eu.metatools.f2d.resource.get
import eu.metatools.f2d.tools.Location
import eu.metatools.f2d.tools.ReferData
import eu.metatools.f2d.tools.ReferText
import eu.metatools.f2d.tools.hashImage
import eu.metatools.f2d.util.centeredX
import eu.metatools.f2d.util.centeredY
import eu.metatools.up.*
import eu.metatools.up.dt.Instruction
import eu.metatools.up.dt.Lx
import eu.metatools.up.dt.div
import eu.metatools.up.dt.lx
import eu.metatools.up.kryo.hashTo
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
 * Solid white.
 */
val solidDrawable by lazy {
    Resources.solid.get()
}

/**
 * Drawable for description.
 */
private val descriptionDrawable by lazy {
    Resources.segoe[ReferText(
        horizontal = Location.Center,
        vertical = Location.Start,
        bold = true
    )].tint(Color.YELLOW)
}

/**
 * Draws two texts so it looks like they have a shadow.
 */
fun InOut.shadowText(on: Drawable<String>, text: String, time: Double, x: Float, y: Float, size: Float, d: Float = 2f) {
    submit(on.tint(Color.BLACK), text, time, Mat.translation(x + d, y - d, uiZ).scale(size))
    submit(on, text, time, Mat.translation(x, y, uiZ).scale(size))
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

        // After creation, also connect the input processor.
        Gdx.input.inputProcessor = object : InputAdapter() {
            override fun keyUp(keycode: Int): Boolean {
                when (keycode) {
                    Keys.ESCAPE -> Gdx.app.exit()
                    else -> return false
                }

                return true
            }
        }

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
     * True if state hashes should be displayed.
     */
    private var debug = false

    /**
     * True if network should be debugged.
     */
    private var debugNet = false

    /**
     * Turns key input into (x, y) coordinates.
     */
    private val keyStick = KeyStick()

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
     * The text drawable.
     */
    private val pingDrawable by lazy {
        Resources.segoe[ReferText(
            horizontal = Location.End,
            vertical = Location.Start
        )].tint(Color.LIGHT_GRAY)
    }

    /**
     * Computed hashes for debug.
     */
    private val hashes = mutableListOf<LifecycleDrawable<Unit?>>()

    /**
     * Current scaling factor.
     */
    private var scaling = 4f

    /**
     * True if left button was down.
     */
    private var wasDown = false

    /**
     * Last direction that was sent to the mover.
     */
    private var lastDir = Dir.Right

    override fun render(time: Double, delta: Double) {
        // Bind current time.
        shell.withTime(clock) {
            // Get own mover (might be non-existent).
            val mover = ownMover()

            // Check if mover is there.
            if (mover == null) {
                // Set to scaling.
                view = Mat.scaling(scaling)

                // If key just pressed, create a new mover.
                if (Gdx.input.isKeyJustPressed(Keys.F1))
                    root.createHero(shell.player)
            } else {
                // Get the coorinate of the mover.
                val (x, y, z) = mover.xyz(time)

                // Center on it.
                view = Mat
                    .translation(Gdx.graphics.width / 2f, Gdx.graphics.height / 2f)
                    .scale(scaling)
                    .translate(x = -x.toFloat() * tileWidth, y = -y.toFloat() * tileHeight)
                    .translate(y = -z.toFloat() * tileHeight)

                // Get desired move direction.
                keyStick.fetch()?.let {
                    // Send to mover if present.
                    mover.move(it.toQ())
                }

                // Get aim direction.
                val dx = Gdx.input.centeredX
                val dy = Gdx.input.centeredY

                // Compute dir from direction and compare against last. If not equal, send new look at.
                val newDir = Dir.from(dx, dy)
                if (newDir != lastDir)
                    mover.lookAt(newDir)

                // Assign new last dir.
                lastDir = newDir

                // If right button just pressed, cancel draw.
                if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT))
                    mover.cancelDraw()

                // Check if left button is down.
                val isDown = Gdx.input.isButtonPressed(Input.Buttons.LEFT)

                // If just pressed, begin draw.
                if (!wasDown && isDown) mover.draw()

                // If just released, end draw.
                if (wasDown && !isDown) mover.release(QPt(dx, dy))

                // Assign new was down.
                wasDown = isDown

                // Render mover parameters.
                ui.submitHealth(mover, time)
                ui.submitDraw(mover, time)
                ui.submitAmmo(mover, time)
                ui.submitXP(mover, time)
            }
        }


        // Dispatch global update.
        root.worldUpdate(clock.time)

        // Render everything.
        shell.list<Rendered>().forEach { it.render(Mat.ID, time) }

        // Check if sign off was set.
        signOffValue?.let {
            // Invalidate to it.
            shell.engine.invalidate(it)

            // Update hash values if debugging.
            if (debug) {
                val target = Hashing.farmHashFingerprint64().newHasher()
                shell.hashTo(target, ::configureKryo)
                val bytes = target.hash().asBytes()
                hashes.add(0, Resources.data[ReferData(bytes, ::hashImage)])
                while (hashes.size > 5)
                    hashes.asReversed().removeAt(0).dispose()
            } else {
                if (hashes.isNotEmpty())
                    hashes.asReversed().removeAt(0).dispose()
            }

            // Reset sign off value.
            signOffValue = null
        }

        // Render if description present.
        (capture?.first as? Described)?.let {
            // Could at this point be disconnected.
            if (it.isConnected())
                ui.shadowText(
                    descriptionDrawable, it.describe, time,
                    Gdx.graphics.width.toFloat() / 2f, Gdx.graphics.height - 16f, 32f
                )
        }

        // Render network offset (ping).
        ui.submit(
            pingDrawable, "Offset: ${clock.currentDeltaTime}ms", time, Mat.translation(
                Gdx.graphics.width.toFloat() - 16f,
                Gdx.graphics.height.toFloat() - 16f,
                -50f
            ).scale(24f)
        )

        // Dispalay all hashes if debugging.
        if (debug) {
            for ((i, h) in hashes.withIndex()) {
                ui.submit(h, time, Mat.translation(32f, 32f + (i * 64f), -40f).scale(48f))
            }
        }

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