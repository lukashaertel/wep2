package eu.metatools.ex

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.Color
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.minlog.Log
import eu.metatools.ex.data.basicMap
import eu.metatools.ex.ents.*
import eu.metatools.ex.ents.Constants.tileHeight
import eu.metatools.ex.ents.Constants.tileWidth
import eu.metatools.ex.ents.hero.*
import eu.metatools.ex.input.KeyStick
import eu.metatools.ex.input.Look
import eu.metatools.ex.input.Mouse
import eu.metatools.fio.InOut
import eu.metatools.fio.data.Mat
import eu.metatools.fio.data.Pt
import eu.metatools.fio.data.Vec
import eu.metatools.fio.drawable.Drawable
import eu.metatools.fio.drawable.tint
import eu.metatools.fig.BaseGame
import eu.metatools.up.dt.Time
import eu.metatools.up.dt.div
import eu.metatools.up.dt.lx
import eu.metatools.up.lang.Bind
import eu.metatools.up.list

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

class EX : BaseGame(64f) {
    override fun configureNet(kryo: Kryo) {
        configureKryo(kryo)
    }


    override fun outputInit() {
        // Set model scaling to display scaled up.
        world.view = Mat.scaling(scaling)

        // Set title.
        Gdx.graphics.setTitle("Joined, player: ${shell.player}")
    }

    /**
     * Root world.
     */
    lateinit var root: World

    override fun shellResolve() {
        root = shell.resolve(lx / "root") as World
    }

    override fun shellCreate() {
        root = World(shell, lx / "root", this, basicMap).also {
            shell.engine.add(it)
        }
    }

    override fun Bind<Time>.shellAlways() {
        root.createHero(shell.player)
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

    /**
     * Current scaling factor.
     */
    private var scaling = 4f

    val world = addLayer(0, layerOrthographic(true))

    val ui = addLayer(1, layerOrthographic(true))

    override fun Bind<Time>.inputShell(time: Double, delta: Double) {
        // Get own mover (might be non-existent).
        val hero = ownMover()

        // Check if mover is there.
        if (hero == null) {
            // Set to scaling.
            world.view = Mat.scaling(scaling)

            // If key just pressed, create a new mover.
            if (Gdx.input.isKeyJustPressed(Keys.F1))
                root.createHero(shell.player)
        } else {
            // Get the coordinate of the mover.
            val pos = hero.posAt(time)
            val (x, y, z) = pos

            // Center on it.
            world.view = Mat
                .translation(Gdx.graphics.width / 2f, Gdx.graphics.height / 2f)
                .scale(scaling, scaling)
                .translate(x = -x * tileWidth, y = -y * tileHeight)
                .translate(y = -z * tileHeight)

            if (Gdx.input.isKeyJustPressed(Keys.SPACE)) {
                if (hero.grounded)
                    hero.jump()
            }

            // Get desired move direction.
            keyStick.current.let {
                if (hero.grounded) {
                    val velFrom = Pt(hero.vel.x, hero.vel.y)
                    val velTo = Pt(it.x.toFloat(), it.y.toFloat())
                    if (velFrom != velTo)
                        hero.move(velTo)
                }
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
                    hero.targetOf(capture?.first, time)?.let { target ->
                        hero.release(target)
                    }
            }

            // Render mover parameters.
            ui.submitHealth(hero, time)
            ui.submitDraw(hero, time)
            ui.submitAmmo(hero, time)
            ui.submitXP(hero, time)
        }
    }

    override fun inputRepeating(timeMs: Long) {
        // Dispatch global update.
        root.worldUpdate(timeMs)
    }

    override fun output(time: Double, delta: Double) {
        // Render everything.
        world.begin()
        shell.list<Rendered>().forEach { it.render(Mat.ID, time) }
        world.end()

        // Submit described elements.
        ui.begin()
        ui.submitDescribed(capture?.first as? Described, time)
        ui.submitPing(netClock, time)

        // Display all hashes if debugging.
        if (debug) ui.submitHashes(shellHashes, time)
        ui.end()

    }

    /**
     * Handles non-game stuff.
     */
    override fun inputOther(time: Double, delta: Double) {
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

    override fun signOff(it: Long) {
        super.signOff(it)

        // Update hash values if debugging.
        if (debug) shellHashes.pushHash(shell) else shellHashes.clear()
    }

    /**
     * Current captured element and position.
     */
    var capture: Pair<Any, Vec>? = null
        private set

    /**
     * True if [any] is the first entry of [capture].
     */
    fun isSelected(any: Any) =
        capture?.first == any

    override fun capture(layer: Layer, result: Any?, relative: Vec, absolute: Vec) {
        if (layer !== world) return

        // Get actual coordinates.
        val (x, y, z) = absolute

        // Memorize result, convert to world space.
        capture = (result ?: root) to Vec(x / tileWidth, y / tileHeight, -z)
    }

}

/**
 * Frontend instance.
 */
lateinit var ex: EX

fun main() {
    // Create the frontend.
    ex = EX()

    // Set config values.
    val config = LwjglApplicationConfiguration()
    config.height = config.width

    // Start application.
    LwjglApplication(ex, config)
}