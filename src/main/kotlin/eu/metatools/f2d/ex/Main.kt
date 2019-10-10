package eu.metatools.f2d.ex

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import eu.metatools.f2d.F2DListener
import eu.metatools.f2d.math.Mat
import eu.metatools.f2d.math.Pt
import eu.metatools.f2d.math.Vec
import eu.metatools.f2d.wep2.encoding.GdxEncoding
import eu.metatools.nw.enter
import eu.metatools.wep2.aspects.wasRestored
import eu.metatools.wep2.entity.name
import eu.metatools.wep2.system.*
import eu.metatools.wep2.util.listeners.Listener
import eu.metatools.wep2.util.listeners.MapListener

// Shortened type declarations as aliases.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

typealias GameName = String
typealias GameParam = Unit
typealias GameSystem = StandardSystem<GameName>
typealias GameEntity = StandardEntity<GameName>
typealias GameContext = StandardContext<GameName>


val Long.sec get() = this / 1000.0

// Frontend object.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

class Frontend : F2DListener(-100f, 100f) {
    val encoding = GdxEncoding<GameName, GameParam>()

    /**
     * Cluster contribution methods.
     */
    val net = enter(
        encoding, "game",
        indexListener = MapListener.console("index")//,
//        playerSelfListener = Listener.console("self"),
//        playerCountListener = Listener.console("count")
    )

    /**
     * The game system.
     */
    val system get() = net.system


    /**
     * The current time of the connected system.
     */
    override val time: Double
        get() = system.toSystemTime(System.currentTimeMillis()).sec

    /**
     * Get or create the world entity.
     */
    val world = if (system.wasRestored)
        system.single()
    else
        World(system, null).apply {
            for (x in 0..10)
                for (y in 0..10) {
                    val xy = XY(x, y)
                    tiles[xy] = if (x in 1..9 && y in 1..9)
                        Tiles.A
                    else
                        Tiles.B
                }

            tiles[XY(3, 2)] = Tiles.B
            tiles[XY(3, 3)] = Tiles.B
            tiles[XY(4, 2)] = Tiles.B
            tiles[XY(4, 3)] = Tiles.B
            tiles[XY(5, 3)] = Tiles.B
        }

    init {
        val t = System.currentTimeMillis()
        system.claimNewPlayer(t)
        world.signal("createMover", system.time(t), system.self)
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

    override fun render(time: Double, delta: Double) {
        // Get current time.
        val current = System.currentTimeMillis()

        val move = keyStick.fetch()
        if (move != null)
            system.firstOrNull<Mover> { it.owner == system.self }
                ?.signal("dir", system.time(current), move)

        // Render the entities, tick if needed.
        for ((_, e) in system.index.toList()) { // todo: index somehow changes from a different thread.
            (e as? Rendered)?.render(time)
            (e as? Ticking)?.ticker?.tickToWith(system, e.name("tick"), current)
        }

        // Consolidate instruction cache.
        system.consolidate(current - 5000L)
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

        system.releasePlayer()

        net.stop()

//        Gdx.files.internal("sg").write(false).use {
//            encoding.writeInitializer(it, system.save())
//        }
    }
}

lateinit var frontend: Frontend

fun main() {
    frontend = Frontend()
    val config = LwjglApplicationConfiguration()
    LwjglApplication(frontend, config)
}