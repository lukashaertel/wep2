package eu.metatools.f2d.ex

import com.badlogic.gdx.Gdx
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
import eu.metatools.f2d.up.kryo.makeGDXKryo
import eu.metatools.up.StandardEngine
import eu.metatools.up.dt.Instruction
import eu.metatools.up.dt.Lx
import eu.metatools.up.dt.div
import eu.metatools.up.dt.lx
import eu.metatools.up.list
import eu.metatools.up.net.NetworkClock
import eu.metatools.up.net.makeNetwork
import eu.metatools.up.receive
import eu.metatools.up.withTime
import java.util.concurrent.CopyOnWriteArrayList

val Long.sec get() = this / 1000.0

// Frontend object.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

class Frontend : F2DListener(-100f, 100f) {
    val encoding = makeF2DKryo().apply {
        register(Movers::class.java, DefaultSerializers.EnumSerializer(Movers::class.java))
        register(Tiles::class.java, DefaultSerializers.EnumSerializer(Tiles::class.java))
    }

    private val lock = Any()

    private fun handleBundle(): Map<Lx, Any?> {
        synchronized(lock) {
            val result = hashMapOf<Lx, Any?>()
            engine.saveTo(result::set)
            return result
        }
    }

    private fun handleReceive(instruction: Instruction) {
        synchronized(lock) {
            engine.receive(instruction)
        }
    }

    val net = makeNetwork("next-cluster", { handleBundle() }, { handleReceive(it) }, kryo = encoding)

    val clock = NetworkClock(net)

    val engine = StandardEngine(net.claimSlot()).also {
        println("As player: ${it.player}")
        it.onTransmit.register(net::instruction)
    }


    /**
     * The current time of the connected system.
     */
    override val time: Double
        get() = (clock.time - engine.initializedTime).sec

    /**
     * Get or create the world entity.
     */
    val world = if (net.isCoordinating) {
        World(engine, lx / "root").also(engine::add).apply {
            for (x in 0..10)
                for (y in 0..10) {
                    val xy = Cell(x, y)
                    tiles[xy] = if (x in 1..9 && y in 1..9)
                        Tiles.A
                    else
                        Tiles.B
                }

            tiles[Cell(3, 2)] = Tiles.B
            tiles[Cell(3, 3)] = Tiles.B
            tiles[Cell(4, 2)] = Tiles.B
            tiles[Cell(4, 3)] = Tiles.B
            tiles[Cell(5, 3)] = Tiles.B
        }
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

    private fun ownMover(): Mover? =
        engine.list<Mover>().find { it.owner == engine.player }

    override fun render(time: Double, delta: Double) {
        synchronized(lock) {
            // Bind current time.
            engine.withTime(clock) {
                val move = keyStick.fetch()
                if (move != null) {
                    val om = ownMover()
                    om?.dir?.invoke(move)
                }

                world.worldUpdate(clock.time)
                engine.invalidate(clock.time - 10_000L)

                engine.list<Rendered>().forEach { it.render(time) }
            }
        }
    }

    override fun capture(result: Any, intersection: Vec) {
    }

    init {
        model = Mat.scaling(2f, 2f)
    }

    override fun pause() = Unit

    override fun resume() = Unit

    override fun dispose() {
        super.dispose()

        net.releaseSlot(engine.player)

        net.close()
    }
}

lateinit var frontend: Frontend

fun main() {
    frontend = Frontend()
    val config = LwjglApplicationConfiguration()
    LwjglApplication(frontend, config)
}