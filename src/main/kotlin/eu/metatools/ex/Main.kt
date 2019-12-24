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
import eu.metatools.f2d.F2DListener
import eu.metatools.ex.data.stupidBox
import eu.metatools.ex.ents.*
import eu.metatools.ex.input.KeyStick
import eu.metatools.ex.math.toPts
import eu.metatools.f2d.context.LifecycleDrawable
import eu.metatools.f2d.context.UI
import eu.metatools.f2d.math.*
import eu.metatools.f2d.tools.*
import eu.metatools.up.*
import eu.metatools.up.dt.*
import eu.metatools.up.kryo.*
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

val Long.sec get() = this / 1000.0

fun Long.toNextFullSecond() =
    1000L - (this % 1000L)

class Frontend : F2DListener(-100f, 100f) {

    private fun handleBundle(): Map<Lx, Any?> {
        val result = hashMapOf<Lx, Any?>()
        shell.store(result::set)
        return result
    }

    private fun handleReceive(instruction: Instruction) {
        shell.receive(instruction)
    }

    val net = makeNetwork("next-cluster", { handleBundle() }, { handleReceive(it) },
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

    @Suppress("experimental_api_usage_error")
    private fun resolveGlobal(param: KParameter): Any {
        if (param.type.isSubtypeOf(typeOf<UI>()) && param.type.isSupertypeOf(typeOf<Frontend>()))
            return this

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
    lateinit var world: World

    private var signOffValue: Long? = null

    var model = Mat.ID

    override fun create() {
        super.create()

        // Set model scaling to display scaled up.
        model = Mat.scaling(2f, 2f)

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

        shell.critical {
            // Assign world from loading or creating.
            world = if (net.isCoordinating) {
                World(shell, lx / "root", this, stupidBox).also {
                    shell.engine.add(it)
                }
            } else {
                // Restore, resolve root.
                val bundle = net.bundle()

                shell.loadFromMap(bundle, ::resolveGlobal, check = true)
                shell.resolve(lx / "root") as World
            }

            shell.withTime(clock) {
                world.createMover(shell.player)
            }
        }
    }

    var debug = false
    var debugNet = false

    var rand = false

    private val keyStick = KeyStick()

    private fun ownMover(): Mover? =
        shell.list<Mover>().find { it.owner == shell.player }

    override fun render() {
        // Block network on all rendering, including sending via Once.
        shell.critical {
            super.render()
        }
    }

    private val generatorRandom = Random()

    /**
     * The text drawable.
     */
    private val segoe by lazy {
        Resources.segoe[ReferText(
            horizontal = Location.Start,
            vertical = Location.End
        )]
    }

    private val descriptionDrawable by lazy {
        Resources.segoe[ReferText(
            horizontal = Location.Center,
            vertical = Location.Start,
            bold = true
        )].tint(Color.YELLOW)
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

    private val hashes = mutableListOf<LifecycleDrawable<Unit?>>()

    private var scaling = Mat.scaling(2f)

    override fun render(time: Double, delta: Double) {
        // Bind current time.
        shell.withTime(clock) {
            // Get own mover (might be non-existent).
            val mover = ownMover()

            // Check if mover is there.
            if (mover == null) {
                model = Mat.ID

                // It is not, allow for recreation.
                if (rand || Gdx.input.isKeyJustPressed(Keys.F1))
                    world.createMover(shell.player)
            } else {
                val (x, y) = mover.posAt(time)
                model = Mat.translation(
                    Gdx.graphics.width / 2f,
                    Gdx.graphics.height / 2f
                ) * scaling * Mat.translation(
                    -x.toFloat() * Constants.tileWidth,
                    -y.toFloat() * Constants.tileHeight
                )

                // Get desired move direction.
                val move = keyStick.fetch()

                // Movement is present, pass to mover.
                if (move != null)
                    mover.moveInDirection(move.toReal())

                // Space was pressed, shot in direction.
                if (Gdx.input.isKeyJustPressed(Keys.SPACE))
                    mover.shoot(null)

                // Mouse is pressed, shoot at target.
                if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                    capture?.let { (result, pos) ->
                        mover.shoot(pos - mover.pos)
                    }
                }

                // Check if randomly playing.
                if (rand) {
                    // In some possibility, shoot.
                    if (generatorRandom.nextDouble() > 0.9)
                        mover.shoot(null)

                    // In some possibility, move.
                    if (generatorRandom.nextDouble() > 0.05) {
                        val rmx = (generatorRandom.nextInt(3) - 1).toReal()
                        val rmy = (generatorRandom.nextInt(3) - 1).toReal()
                        mover.moveInDirection(RealPt(rmx, rmy))
                    }

                }
            }
        }


        // Dispatch global update.
        world.worldUpdate(clock.time)

        // Render everything.
        shell.list<Rendered>().forEach { it.render(model, time) }

        // Check if sign off was set.
        signOffValue?.let {
            // Invalidate to it.
            shell.engine.invalidate(it)

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

            // Reset.
            signOffValue = null
        }

        //TODO: Fix UI cooordinate system.

        submit(segoe, "${world.res} shared resources", time, Mat.translation(60f, 10f, -50f).scale(24f))

        (capture?.first as? HasDescription)?.let {
            submit(
                descriptionDrawable, it.describe, time, Mat.translation(
                    Gdx.graphics.width.toFloat() / 2f,
                    Gdx.graphics.height.toFloat() - 16f,
                    -50f
                ).scale(32f)
            )
        }
        submit(
            pingDrawable, "Offset: ${clock.currentDeltaTime}ms", time, Mat.translation(
                Gdx.graphics.width.toFloat() - 16f,
                Gdx.graphics.height.toFloat() - 16f,
                -50f
            ).scale(24f)
        )


        if (debug) {
            for ((i, h) in hashes.withIndex()) {
                submit(h, time, Mat.translation(32f, 32f + (i * 64f), -40f).scale(48f))
            }
        }

        val matTS = model.translate(z = -30f).scale(Constants.tileWidth, Constants.tileHeight)

        val om = ownMover()
        if (om != null) {
            val plus = world.hull[om.level]?.plus.orEmpty() +
                    world.clip[om.level]?.plus.orEmpty()

            val minus = world.hull[om.level]?.minus.orEmpty() +
                    world.clip[om.level]?.minus.orEmpty()

            for (p in plus) {
                val c = p.avg()
                submit(
                    Resources.shapes[ReferPoly(p.map { it - c }.toPts())],
                    time, matTS.translate(c.x.toFloat(), c.y.toFloat())
                )
            }
            for (p in minus) {
                val c = p.avg()
                submit(
                    Resources.shapes[ReferPoly(p.map { it - c }.toPts())].tint(Color.RED),
                    time, matTS.translate(c.x.toFloat(), c.y.toFloat())
                )
            }
        }

        for (r in world.entries.values) {
            for (p in r.plus) {
                val c = p.avg()
                submit(
                    Resources.shapes[ReferPoly(p.map { it - c }.toPts())].tint(Color.CYAN),
                    time, matTS.translate(c.x.toFloat(), c.y.toFloat())
                )
            }
        }

        // Process non-game input.
        if (Gdx.input.isKeyJustPressed(Keys.NUM_1))
            scaling = Mat.scaling(1f, 1f)
        if (Gdx.input.isKeyJustPressed(Keys.NUM_2))
            scaling = Mat.scaling(2f, 2f)
        if (Gdx.input.isKeyJustPressed(Keys.NUM_3))
            scaling = Mat.scaling(3f, 3f)

        if (Gdx.input.isKeyJustPressed(Keys.R))
            rand = !rand

        if (Gdx.input.isKeyJustPressed(Keys.F9))
            debug = !debug

        if (Gdx.input.isKeyJustPressed(Keys.F10)) {
            debugNet = !debugNet
            Log.set(if (debugNet) Log.LEVEL_DEBUG else Log.LEVEL_INFO)
        }
    }

    private var capture: Pair<Any, RealPt>? = null

    fun isSelected(any: Any) =
        capture?.first == any

    override fun capture(result: Any?, intersection: Vec) {
        val (x, y) = model.inv * intersection

        // Memorize result.
        capture = (result ?: world) to RealPt(x / Constants.tileWidth, y / Constants.tileHeight)
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

lateinit var frontend: Frontend

fun main() {
    frontend = Frontend()
    val config = LwjglApplicationConfiguration()
    config.height = config.width
    LwjglApplication(frontend, config)
}