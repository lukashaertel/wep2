package eu.metatools.f2d.ex

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.esotericsoftware.kryo.serializers.DefaultSerializers
import eu.metatools.f2d.F2DListener
import eu.metatools.f2d.context.refer
import eu.metatools.f2d.math.Cell
import eu.metatools.f2d.math.Mat
import eu.metatools.f2d.math.Vec
import eu.metatools.f2d.tools.Location
import eu.metatools.f2d.tools.ReferText
import eu.metatools.f2d.tools.TextResource
import eu.metatools.f2d.tools.findDefinitions
import eu.metatools.f2d.up.kryo.makeF2DKryo
import eu.metatools.up.StandardEngine
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
import kotlin.math.cos
import kotlin.math.sin

val Long.sec get() = this / 1000.0

class Frontend : F2DListener(-100f, 100f) {
    val encoding = makeF2DKryo().apply {
        register(Movers::class.java, DefaultSerializers.EnumSerializer(Movers::class.java))
        register(Tiles::class.java, DefaultSerializers.EnumSerializer(Tiles::class.java))
    }

    private fun handleBundle(): Map<Lx, Any?> {
        val result = hashMapOf<Lx, Any?>()
        engine.saveTo(result::set)
        return result
    }

    private fun handleReceive(instruction: Instruction) {
        engine.receive(instruction)
    }

    val net = makeNetwork("next-cluster", { handleBundle() }, { handleReceive(it) },
        kryo = encoding,
        leaseTime = 60,
        leaseTimeUnit = TimeUnit.SECONDS
    )

    val clock = NetworkClock(net)

    val claimer = NetworkClaimer(net, UUID.randomUUID())

    val engine = StandardEngine(claimer.currentClaim).also {
        it.onTransmit.register(net::instruction)
    }


    /**
     * The current time of the connected system.
     */
    override val time: Double
        get() = (clock.time - engine.initializedTime).sec

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
    }.toMap()

    /**
     * Get or create the world entity.
     */
    val world = if (net.isCoordinating) {
        World(engine, lx / "root", map).also(engine::add)
    } else {
        // Restore, resolve root.
        val bundle = net.bundle()
        engine.loadFrom(bundle::get)
        engine.resolve(lx / "root") as World
    }

    init {
        engine.withTime(clock) {
            world.createMover(engine.player)
        }
    }

    override fun create() {
        super.create()
        model = Mat.scaling(2f, 2f)

        Gdx.graphics.setTitle("Joined, player: ${engine.player}")

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

    private val keyStick = KeyStick()
    var fontSize = Constants.tileHeight / 2f
    private fun ownMover(): Mover? =
        engine.list<Mover>().find { it.owner == engine.player }

    override fun render(time: Double, delta: Double) {
        if (engine.player != claimer.currentClaim)
            System.err.println("Warning: Claim for engine has changed, this should not happen.")

        // Bind current time.
        engine.withTime(clock) {
            // If coordinator, responsible for disposing of now unclaimed IDs.
            if (net.isCoordinating)
                world.movers.forEach {
                    if (!it.dead && !net.isClaimed(it.owner))
                        it.kill()
                }

            val move = keyStick.fetch()
            if (move != null) {
                val om = ownMover()
                om?.dir?.invoke(move)
            }

            world.worldUpdate(clock.time)
            engine.invalidate(clock.time - 10_000L)

            engine.list<Rendered>().forEach { it.render(time) }
        }

        if (Gdx.input.isKeyJustPressed(Keys.NUM_1))
            model = Mat.scaling(1f, 1f)
        if (Gdx.input.isKeyJustPressed(Keys.NUM_2))
            model = Mat.scaling(2f, 2f)
        if (Gdx.input.isKeyJustPressed(Keys.NUM_3))
            model = Mat.scaling(3f, 3f)
    }

    override fun capture(result: Any, intersection: Vec) {
        println(result)
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

lateinit var frontend: Frontend

fun main() {
    frontend = Frontend()
    val config = LwjglApplicationConfiguration()
    LwjglApplication(frontend, config)
}