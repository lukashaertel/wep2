package eu.metatools.ex

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.serializers.DefaultSerializers
import com.esotericsoftware.minlog.Log
import com.google.common.hash.Hashing
import eu.metatools.f2d.F2DListener
import eu.metatools.ex.data.stupidBox
import eu.metatools.ex.ents.*
import eu.metatools.ex.input.KeyStick
import eu.metatools.f2d.math.Cell
import eu.metatools.f2d.math.Mat
import eu.metatools.f2d.math.Vec
import eu.metatools.f2d.up.kryo.registerF2DSerializers
import eu.metatools.f2d.up.kryo.registerGDXSerializers
import eu.metatools.up.*
import eu.metatools.up.dt.*
import eu.metatools.up.kryo.*
import eu.metatools.up.net.NetworkClaimer
import eu.metatools.up.net.NetworkClock
import eu.metatools.up.net.NetworkSignOff
import eu.metatools.up.net.makeNetwork
import java.util.*
import java.util.concurrent.TimeUnit

val Long.sec get() = this / 1000.0


class Frontend : F2DListener(-100f, 100f) {
    private fun configureKryo(kryo: Kryo) {
        // Add basic serialization.
        setDefaults(kryo)
        registerKotlinSerializers(kryo)
        registerUpSerializers(kryo)

        // Add graphics serialization.
        registerGDXSerializers(kryo)
        registerF2DSerializers(kryo)

        // Register data objects.
        kryo.register(
            Movers::class.java, DefaultSerializers.EnumSerializer(
                Movers::class.java
            )
        )
        kryo.register(
            Tiles::class.java, DefaultSerializers.EnumSerializer(
                Tiles::class.java
            )
        )
    }

    private fun handleBundle(): Map<Lx, Any?> {
        val result = hashMapOf<Lx, Any?>()
        shell.store(result::set)
        return result
    }

    private fun handleReceive(instruction: Instruction) {
        shell.receive(instruction)
    }

    val net = makeNetwork("next-cluster", { handleBundle() }, { handleReceive(it) },
        leaseTime = 60,
        leaseTimeUnit = TimeUnit.SECONDS,
        configureKryo = ::configureKryo
    )

    /**
     * Network clock.
     */
    private val clock = NetworkClock(net)

    /**
     * Network player claimer. Claims and holds a player ID per UUID.
     *
     * TODO: Expiry is still a bit fucked.
     */
    private val claimer = NetworkClaimer(net, UUID.randomUUID(), changed = { old, new ->
        System.err.println("Warning: Claim for engine has changed from $old to $new, this should not happen.")
    })

    /**
     * Sign-off coordinator.
     */
    private val signOff = NetworkSignOff(net, changed = { old, new ->
        println("Sign-off moved from $old to $new")
        if (new != null)
            signOffValue = new
    })

    /**
     * The shell that runs the game.
     */
    val shell = StandardShell(claimer.currentClaim).also {
        it.onTransmit = net::instruction
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

        // Assign world from loading or creating.
        world = if (net.isCoordinating) {
            World(shell, lx / "root", stupidBox).also {
                shell.engine.add(it)
            }
        } else {
            // Restore, resolve root.
            val bundle = net.bundle()
            shell.critical {
                shell.loadFromMap(bundle, true)
            }
            shell.resolve(lx / "root") as World
        }

        shell.withTime(clock) {
            world.createMover(shell.player)
        }
    }

    var debug = false
    var debugNet = false

    var rand = false

    private val keyStick = KeyStick()

    var fontSize = Constants.tileHeight / 2f

    private fun ownMover(): Mover? =
        shell.list<Mover>().find { it.owner == shell.player }

    override fun render() {
        // Block network on all rendering, including sending via Once.
        shell.critical {
            super.render()
        }
    }

    private val generatorRandom = Random()

    override fun render(time: Double, delta: Double) {
        // Bind current time.
        shell.withTime(clock) {
            // Get own mover (might be non-existent).
            val mover = ownMover()

            // Check if mover is there.
            if (mover == null) {
                // It is not, allow for recreation.
                if (Gdx.input.isKeyJustPressed(Keys.F1))
                    world.createMover(shell.player)

                // If randomly playing, always create new.
                if (rand)
                    world.createMover(shell.player)
            } else {
                // Get desired move direction.
                val move = keyStick.fetch()

                // Movement is present, pass to mover.
                if (move != null)
                    mover.moveInDirection(move)

                // Space was pressed, shot in direction.
                if (Gdx.input.isKeyJustPressed(Keys.SPACE))
                    mover.shoot(null)

                // Mouse is pressed, shoot at target.
                if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT))
                    selectedMover?.takeIf { it.driver.isConnected }?.let { o ->
                        mover.shoot(o.pos - mover.pos)
                    }

                // Check if randomly playing.
                if (rand) {
                    // In some possibility, shoot.
                    if (generatorRandom.nextDouble() > 0.9)
                        mover.shoot(null)

                    // In some possibility, move.
                    if (generatorRandom.nextDouble() > 0.4)
                        mover.moveInDirection(Cell(generatorRandom.nextInt(3) - 1, generatorRandom.nextInt(3) - 1))
                }
            }
        }


        // Dispatch global update.
        world.worldUpdate(clock.time)

        // Render everything.
        shell.list<Rendered>().forEach { it.render(time) }

        // Check if sign off was set.
        signOffValue?.let {
            // Invalidate to it.
            shell.engine.invalidate(it)

            if (debug) {
                // Store engine.
                val map = TreeMap<Lx, Any?>()
                shell.store(map::set)
                println(map)

                // Hash engine.
                val kryoPool = KryoConfiguredPool(::configureKryo, false)
                val hash = Hashing
                    .sha512()
                    .newHasher()
                    .putObject(map, KryoFunnel(kryoPool))
                    .hash()

                // Print hash.
                println(hash)
            }

            // Reset.
            signOffValue = null
        }


        // Process non-game input.
        if (Gdx.input.isKeyJustPressed(Keys.NUM_1))
            model = Mat.scaling(1f, 1f)
        if (Gdx.input.isKeyJustPressed(Keys.NUM_2))
            model = Mat.scaling(2f, 2f)
        if (Gdx.input.isKeyJustPressed(Keys.NUM_3))
            model = Mat.scaling(3f, 3f)

        if (Gdx.input.isKeyJustPressed(Keys.R))
            rand = !rand

        if (Gdx.input.isKeyJustPressed(Keys.F9))
            debug = !debug

        if (Gdx.input.isKeyJustPressed(Keys.F10)) {
            debugNet = !debugNet
            Log.set(if (debugNet) Log.LEVEL_DEBUG else Log.LEVEL_INFO)
        }

    }

    var selectedMover: Mover? = null

    override fun capture(result: Any, intersection: Vec) {
        // Result is a mover, memorize it.
        if (result is Mover)
            selectedMover = result
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
    LwjglApplication(frontend, config)
}