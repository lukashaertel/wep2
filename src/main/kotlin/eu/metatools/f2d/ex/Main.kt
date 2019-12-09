package eu.metatools.f2d.ex

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.esotericsoftware.kryo.serializers.DefaultSerializers
import eu.metatools.f2d.F2DListener
import eu.metatools.f2d.math.Cell
import eu.metatools.f2d.math.Mat
import eu.metatools.f2d.math.Vec
import eu.metatools.f2d.up.kryo.makeF2DKryo
import eu.metatools.up.StandardShell
import eu.metatools.up.deb.LogShell
import eu.metatools.up.dt.Instruction
import eu.metatools.up.dt.Lx
import eu.metatools.up.dt.div
import eu.metatools.up.dt.lx
import eu.metatools.up.list
import eu.metatools.up.net.NetworkClaimer
import eu.metatools.up.net.NetworkClock
import eu.metatools.up.net.makeNetwork
import eu.metatools.up.receive
import eu.metatools.up.withTime
import java.util.*
import java.util.concurrent.TimeUnit

val Long.sec get() = this / 1000.0

class Frontend : F2DListener(-100f, 100f) {
    val encoding = makeF2DKryo().apply {
        register(Movers::class.java, DefaultSerializers.EnumSerializer(Movers::class.java))
        register(Tiles::class.java, DefaultSerializers.EnumSerializer(Tiles::class.java))
    }

    private fun handleBundle(): Map<Lx, Any?> {
        val result = hashMapOf<Lx, Any?>()
        shell.on.saveTo(result::set)
        return result
    }

    private fun handleReceive(instruction: Instruction) {
        shell.on.receive(instruction)
    }

    val net = makeNetwork("next-cluster", { handleBundle() }, { handleReceive(it) },
        kryo = encoding,
        leaseTime = 60,
        leaseTimeUnit = TimeUnit.SECONDS
    )

    // TODO: Expiry is still a bit fucked.

    val clock = NetworkClock(net)

    val claimer = NetworkClaimer(net, UUID.randomUUID())

    val shell = LogShell(StandardShell(claimer.currentClaim).also {
        it.onTransmit.register(net::instruction)
    })


    /**
     * The current time of the connected system.
     */
    override val time: Double
        get() = (clock.time - shell.initializedTime).sec

    val map = mutableMapOf<Cell, TileKind>().also {
        for (x in 0..10)
            for (y in 0..10) {
                val xy = Cell(x, y)
                it[xy] = if (x in 1..9 && y in 1..9)
                    Tiles.A
                else
                    Tiles.B
            }

        it[Cell(3, 2)] = Tiles.B
        it[Cell(3, 3)] = Tiles.B
        it[Cell(4, 2)] = Tiles.B
        it[Cell(4, 3)] = Tiles.B
        it[Cell(5, 3)] = Tiles.B
        it[Cell(5, 6)] = Tiles.B
    }.toMap()

    /**
     * Get or create the world entity.
     */
    val world = if (net.isCoordinating) {
        World(shell, lx / "root", map).also {
            shell.engine.add(it)
        }
    } else {
        // Restore, resolve root.
        val bundle = net.bundle()
        shell.on.loadFrom(bundle::get)
        shell.resolve(lx / "root") as World
    }

    init {
        shell.withTime(clock) {
            world.createMover(shell.player)
        }
    }

    override fun create() {
        super.create()
        frontendReady = true
        model = Mat.scaling(2f, 2f)

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
    }

    var consoleVisible = false

    private val keyStick = KeyStick()
    var fontSize = Constants.tileHeight / 2f
    private fun ownMover(): Mover? =
        shell.list<Mover>().find { it.owner == shell.player }

    override fun render(time: Double, delta: Double) {
        if (shell.player != claimer.currentClaim)
            System.err.println("Warning: Claim for engine has changed, this should not happen.")

        // Bind current time.
        shell.withTime(clock) {
            // If coordinator, responsible for disposing of now unclaimed IDs.
            if (net.isCoordinating)
                world.movers.forEach {
                    if (!it.dead && !net.isClaimed(it.owner))
                        it.kill()
                }

            ownMover()?.let {
                val move = keyStick.fetch()
                if (move != null)
                    it.dir(move)
                if (Gdx.input.isKeyJustPressed(Keys.SPACE))
                    it.shoot(null)

                if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT))
                    selectedMover?.takeIf { it.driver.isConnected }?.let { o ->
                        it.shoot(o.pos - it.pos)
                    }
            } ?: run {
                if (Gdx.input.isKeyJustPressed(Keys.F1))
                    world.createMover(shell.player)
            }


            world.worldUpdate(clock.time)
            shell.engine.invalidate(clock.time - 10_000L)

            shell.list<Rendered>().forEach { it.render(time) }
        }

        if (Gdx.input.isKeyJustPressed(Keys.NUM_1))
            model = Mat.scaling(1f, 1f)
        if (Gdx.input.isKeyJustPressed(Keys.NUM_2))
            model = Mat.scaling(2f, 2f)
        if (Gdx.input.isKeyJustPressed(Keys.NUM_3))
            model = Mat.scaling(3f, 3f)

        if (Gdx.input.isKeyJustPressed(Keys.GRAVE))
            consoleVisible = !consoleVisible
    }

    var selectedMover: Mover? = null

    override fun capture(result: Any, intersection: Vec) {
        if (result is Mover)
            selectedMover = result
    }

    override fun pause() = Unit

    override fun resume() = Unit

    override fun dispose() {
        super.dispose()

        clock.close()
        claimer.close()
        net.close()
    }
}

var frontendReady = false
lateinit var frontend: Frontend

fun main() {
    frontend = Frontend()
    val config = LwjglApplicationConfiguration()
    LwjglApplication(frontend, config)
}